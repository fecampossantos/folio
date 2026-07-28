package android.net

import android.os.Parcel

/**
 * Concrete Uri stub for local JVM unit testing without native Android framework runtime.
 *
 * @property uriString String representation of the test URI.
 */
class TestUri(private val uriString: String = "content://test/book.epub") : Uri() {

    override fun isHierarchical(): Boolean = false

    override fun isOpaque(): Boolean = false

    override fun isRelative(): Boolean = false

    override fun getScheme(): String = "content"

    override fun getSchemeSpecificPart(): String = "//test/book.epub"

    override fun getEncodedSchemeSpecificPart(): String = "//test/book.epub"

    override fun getAuthority(): String = "test"

    override fun getEncodedAuthority(): String = "test"

    override fun getPath(): String = "/book.epub"

    override fun getEncodedPath(): String = "/book.epub"

    override fun getPathSegments(): List<String> = listOf("book.epub")

    override fun getLastPathSegment(): String = "book.epub"

    override fun getQuery(): String? = null

    override fun getEncodedQuery(): String? = null

    override fun getFragment(): String? = null

    override fun getEncodedFragment(): String? = null

    override fun getUserInfo(): String? = null

    override fun getEncodedUserInfo(): String? = null

    override fun getHost(): String? = "test"

    override fun getPort(): Int = -1

    override fun getQueryParameters(key: String?): List<String> = emptyList()

    override fun getQueryParameter(key: String?): String? = null

    override fun getBooleanQueryParameter(key: String?, defaultValue: Boolean): Boolean = defaultValue

    override fun normalizeScheme(): Uri = this

    override fun buildUpon(): Builder? = null

    override fun toString(): String = uriString

    override fun compareTo(other: Uri?): Int = 0

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {}
}
