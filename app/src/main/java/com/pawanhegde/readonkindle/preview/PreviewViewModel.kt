package com.pawanhegde.readonkindle.preview

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.*
import com.chimbori.crux.articles.Article
import com.chimbori.crux.articles.ArticleExtractor
import com.pawanhegde.readonkindle.db.ContentDao
import com.pawanhegde.readonkindle.db.ContentDatabase
import com.pawanhegde.readonkindle.entities.Content
import j2html.TagCreator
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

class PreviewViewModel(application: Application) : ViewModel() {
    private val _tag: String = "PreviewViewModel"

    private val contentDao: ContentDao = ContentDatabase.getDatabase(application).contentDao()

    val url: MutableLiveData<String> = MutableLiveData()
    val content: LiveData<Content> = url.switchMap {
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(populateContent(url.value!!))
        }
    }

    private fun populateContent(url: String): Content {
        Log.e(_tag, "--- IN POPULATE CONTENT [${url}]---")

        return fetchContentFromCache(url)
                ?: fetchContentFromInternet(url).also { it.let { contentDao.insert(it) } }

    }

    private fun fetchContentFromCache(url: String): Content? {
        return try {
            contentDao.getByUuid(hash(url))
        } catch (e: Exception) {
            Log.w(_tag, "Failed to find content for $url in cache")
            null
        }
    }

    private fun fetchContentFromInternet(url: String): Content {
        Log.i(_tag, "Fetching from url $url")

        return with(fetchDocument(url)) {
            Content(
                    uuid = hash(url),
                    resolvedUrl = this.location(),
                    title = this.title(),
                    originalContent = this.html(),
                    simplifiedContent = extractReadableContent(this))
        }
    }

    private fun fetchDocument(url: String): Document {
        for (i in 1..5) {
            try {
                val connection = Jsoup.connect(url)
                return connection.get()
            } catch (e: IOException) {
                Log.e(_tag, "Failed to fetch the document at the URL: $url", e)

                if (i == 5) {
                    throw e
                }
            }
        }

        throw RuntimeException("Finished the fetch document loop with neither an exception or a document")
    }


    // TODO: Handle images and other non-text content
    private fun extractReadableContent(document: Document): String {
        // TODO: Replace with Mozilla's engine (or less preferably, create your own)
        val article = ArticleExtractor.with(url.value, document.html())
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
                TagCreator.a("Click here").withHref(url.value),
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
            Log.e(_tag, "When trying to fetch the image, found the exception: $e")
        }

        return null
    }

    private fun hash(string: String): String {
        return String(MessageDigest.getInstance("SHA-512").digest(string.toByteArray()))
    }
}

class PreviewViewModelFactory(private val application: Application, private val targetUrl: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PreviewViewModel(application) as T
    }
}
