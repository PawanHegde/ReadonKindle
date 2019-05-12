package com.pawanhegde.readonkindle.preview

import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chimbori.crux.articles.Article
import com.chimbori.crux.articles.ArticleExtractor
import com.pawanhegde.readonkindle.models.ReadableDocument
import j2html.TagCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL

class PreviewViewModel(private val url: String) : ViewModel() {

    val readableDocument: MutableLiveData<ReadableDocument> by lazy {
        MutableLiveData<ReadableDocument>().also {
            fetchReadableDocument(url)
        }
    }


    private fun fetchReadableDocument(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedDocument = fetchDocument(url)
            val readableContent = extractReadableContent(fetchedDocument)

            val document = ReadableDocument(
                    title = fetchedDocument.title(),
                    url = fetchedDocument.location(),
                    htmlContent = readableContent)

            CoroutineScope(Dispatchers.Main).launch {
                readableDocument.value = document
            }
        }
    }


    private fun fetchDocument(url: String): Document {
        for (i in 1..5) {
            try {
                val connection = Jsoup.connect(url)
                return connection.get()
            } catch (e: IOException) {
                Log.e(this.javaClass.name, "Failed to fetch the document at the URL: $url", e)

                if (i == 5) {
                    throw e
                }
            }
        }

        throw RuntimeException("Finished the fetch document loop with neither an exception or a document")
    }


    private fun extractReadableContent(document: Document): String {
        val article = ArticleExtractor.with(url, document.html())
                .extractMetadata()
                .extractContent()
                .article()

        return createHtml(article)
    }

    private fun createHtml(article: Article): String {
        val encodedImage = when {
            article.imageUrl == null -> null
            article.imageUrl.startsWith("data:") -> article.imageUrl
            else -> "data:image/gif;base64,${getByteArrayFromImageURL(article.imageUrl)}"
        }

        val title = TagCreator.title(article.title)
        val charset = TagCreator.meta().withCharset("UTF-8")
        val meta = TagCreator.meta()
                .attr("http-equiv", "Content-Type")
                .attr("content", "text/html; charset=utf-8")
                .attr("viewport", "width=device-width, initial-scale=1")


        val heading = TagCreator.h1(article.title)
        val description = TagCreator.em(article.description)
        val coverImage = TagCreator.img().withSrc(encodedImage).withStyle("width: 100%")
        val content = TagCreator.rawHtml(article.document.toString())
        val linkToOriginal = TagCreator.p(
                TagCreator.a("Click here").withHref(url),
                TagCreator.span("to view the original page in Kindle's default browser"))

        val head = TagCreator.head(
                title,
                charset,
                meta
        )

        val body = TagCreator.body()
                .with(heading)
                .condWith(!article.description.isNullOrBlank(), description)
                .condWith(!encodedImage.isNullOrBlank(), coverImage)
                .with(content)
                .with(linkToOriginal)

        return TagCreator.html(head, body).renderFormatted()
    }

    private fun getByteArrayFromImageURL(url: String): String? {
        try {
            val imageUrl = URL(url)
            val ucon = imageUrl.openConnection()
            val `is` = ucon.getInputStream()
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var read: Int
            do {
                read = `is`.read(buffer, 0, buffer.size)
                if (read != -1)
                    baos.write(buffer, 0, read)
            } while (read != -1)
            baos.flush()
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            println("When trying to fetch the image, found the exception: $e")
        }

        return null
    }
}

class PreviewViewModelFactory(private val url: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PreviewViewModel(url) as T
    }
}