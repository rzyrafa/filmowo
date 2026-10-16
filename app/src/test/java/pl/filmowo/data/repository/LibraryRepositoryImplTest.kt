package pl.filmowo.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.FakeLibraryDao
import pl.filmowo.testutil.FakePosterStorage
import pl.filmowo.testutil.metadata

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryImplTest {

    private val dao = FakeLibraryDao()
    private val posters = FakePosterStorage()
    private var now = 1_000L

    private fun TestScope.repository() = LibraryRepositoryImpl(dao, posters, { now }, backgroundScope)

    @Test
    fun `adding item stores metadata and downloads poster`() = runTest {
        val repository = repository()
        repository.setStatus(metadata(id = 1), WatchStatus.PLANNED)
        runCurrent()

        val item = repository.observeItem(1, MediaType.MOVIE).first()!!
        assertEquals(WatchStatus.PLANNED, item.status)
        assertEquals("Tytuł 1", item.metadata.title)
        assertEquals(1_000L, item.addedAt)
        assertNull(item.watchedAt)
        assertEquals("/files/posters/poster.jpg", item.localPosterFile)
        assertEquals(listOf("/p1.jpg"), posters.downloads)
    }

    @Test
    fun `changing status keeps rating and added date`() = runTest {
        val repository = repository()
        repository.setStatus(metadata(id = 1), WatchStatus.PLANNED)
        repository.setRating(1, MediaType.MOVIE, 8)
        now = 2_000L
        repository.setStatus(metadata(id = 1), WatchStatus.WATCHED)
        runCurrent()

        val item = repository.observeItem(1, MediaType.MOVIE).first()!!
        assertEquals(WatchStatus.WATCHED, item.status)
        assertEquals(8, item.rating)
        assertEquals(1_000L, item.addedAt)
        assertEquals(2_000L, item.watchedAt)
        assertEquals(2_000L, item.updatedAt)
    }

    @Test
    fun `poster is downloaded only once`() = runTest {
        val repository = repository()
        repository.setStatus(metadata(id = 1), WatchStatus.PLANNED)
        runCurrent()
        repository.setStatus(metadata(id = 1), WatchStatus.WATCHED)
        repository.refreshMetadata(metadata(id = 1))
        runCurrent()
        assertEquals(1, posters.downloads.size)
    }

    @Test
    fun `failed poster download is retried on refresh`() = runTest {
        val repository = repository()
        posters.result = null
        repository.setStatus(metadata(id = 1), WatchStatus.PLANNED)
        runCurrent()
        posters.result = "/files/posters/movie_1.jpg"
        repository.refreshMetadata(metadata(id = 1))
        runCurrent()

        assertEquals(2, posters.downloads.size)
        assertEquals("/files/posters/movie_1.jpg", repository.observeItem(1, MediaType.MOVIE).first()!!.localPosterFile)
    }

    @Test
    fun `refresh updates metadata only for items in library`() = runTest {
        val repository = repository()
        repository.refreshMetadata(metadata(id = 5, title = "Nowy"))
        assertTrue(dao.items.value.isEmpty())

        repository.setStatus(metadata(id = 1, title = "Stary"), WatchStatus.PLANNED)
        repository.refreshMetadata(metadata(id = 1, title = "Nowy"))
        assertEquals("Nowy", repository.observeItem(1, MediaType.MOVIE).first()!!.metadata.title)
    }

    @Test
    fun `remove deletes row and local poster`() = runTest {
        val repository = repository()
        repository.setStatus(metadata(id = 1), WatchStatus.WATCHED)
        runCurrent()
        repository.remove(1, MediaType.MOVIE)

        assertNull(repository.observeItem(1, MediaType.MOVIE).first())
        assertEquals(listOf("/files/posters/poster.jpg"), posters.deleted)
    }

    @Test
    fun `same id for movie and show are separate items`() = runTest {
        val repository = repository()
        repository.setStatus(metadata(id = 7, type = MediaType.MOVIE), WatchStatus.PLANNED)
        repository.setStatus(metadata(id = 7, type = MediaType.TV), WatchStatus.WATCHED)
        assertEquals(2, repository.observeLibrary().first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rating outside 1-10 is rejected`() = runTest {
        repository().setRating(1, MediaType.MOVIE, 11)
    }
}
