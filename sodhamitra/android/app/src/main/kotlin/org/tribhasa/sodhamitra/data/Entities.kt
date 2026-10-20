package org.tribhasa.sodhamitra.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** kind of a research project: a PhD thesis project or an individual researcher / author book project. */
object ProjectKind {
    const val PHD = "PHD"
    const val BOOK = "BOOK"
}

/** the three kinds of bibliographic source (PhD bibliography entries and citation sources). */
object SourceType {
    const val BOOK = "BOOK"
    const val MSS = "MSS"
    const val ARTICLE = "ARTICLE"
}

/** where a manuscript record lives: PhD bibliography, a citation source of a book project, or the Future Editing List. */
object MsScope {
    const val BIB = "BIB"
    const val CIT = "CIT"
    const val EDIT = "EDIT"
}

object PlannerList {
    const val EDITING = "EDITING"
    const val TRANS = "TRANS"
}

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
)

@Entity(
    tableName = "points",
    foreignKeys = [ForeignKey(Chapter::class, ["id"], ["chapterId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("chapterId")],
)
data class Point(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val title: String,
)

/** The description of one point: its own record, linked to the point, holding text of any length. */
@Entity(
    tableName = "point_descriptions",
    foreignKeys = [ForeignKey(Point::class, ["id"], ["pointId"], onDelete = ForeignKey.CASCADE)],
)
data class PointDescription(
    @PrimaryKey val pointId: Long,
    val text: String,
)

@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
)

@Entity(
    tableName = "note_descriptions",
    foreignKeys = [ForeignKey(Note::class, ["id"], ["noteId"], onDelete = ForeignKey.CASCADE)],
)
data class NoteDescription(
    @PrimaryKey val noteId: Long,
    val text: String,
)

/** A book in a PhD bibliography, or a book that is cited in a book project. */
@Entity(
    tableName = "bib_books",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class BibBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val author: String = "",
    val editor: String = "",
    val edition: String = "",
    val publisher: String = "",
    val place: String = "",
    val year: String = "",
)

@Entity(
    tableName = "bib_articles",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class BibArticle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val author: String = "",
    val journal: String = "",
    val volumeIssue: String = "",
    val year: String = "",
    val pages: String = "",
    val issn: String,
    val scopusIndexed: Boolean = false,
    val scopusDetails: String = "",
    val impactFactor: String = "",
)

/**
 * The shared manuscript record (fourteen fields, in the approved order, plus the three independent planning
 * classifications used only by the Future Editing List).
 */
@Entity(
    tableName = "manuscripts",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("scope")],
)
data class Manuscript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scope: String,
    val projectId: Long? = null,
    val serialNo: String = "",          // 1 Mss Serial No.
    val accessionNo: String = "",       // 2 Mss Accession No.
    val title: String,                  // 3 Mss Title (the only mandatory field)
    val stakeholder: String = "",       // 4 Stakeholder
    val place: String = "",             // 5 Place (mandatory only when Stakeholder is filled)
    val edited: String = "",            // 6 Edited / Not Edited  ("", EDITED, NOT_EDITED)
    val author: String = "",            // 7 Author / Attributed Author
    val language: String = "",          // 8 Language
    val script: String = "",            // 9 Script
    val material: String = "",          // 10 Material ("", PALM_LEAF, PAPER, OTHER)
    val materialOther: String = "",     //    detail when material is OTHER
    val folioCount: String = "",        // 11 Folio Count
    val completeness: String = "",      // 12 Completeness and Condition
    val catalogueRef: String = "",      // 13 Catalogue Reference
    val remarks: String = "",           // 14 Remarks
    val published: Boolean = false,     // Future Editing List only
    val unpublished: Boolean = false,
    val toBeTranslated: Boolean = false,
)

/** A citation: where a source is cited (page or folio) and why. The source itself is stored once and shared. */
@Entity(
    tableName = "citations",
    foreignKeys = [ForeignKey(Project::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class Citation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sourceType: String,
    val sourceId: Long,
    val pageFolio: String = "",
    val contextNote: String = "",
)

/** A book of the Future Editing List or the Future Trans List (the same fourteen fields in both). */
@Entity(tableName = "planner_books", indices = [Index("listType")])
data class PlannerBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listType: String,
    val title: String,                          // 1 Title of Book (mandatory)
    val author: String = "",                    // 2
    val editor: String = "",                    // 3
    val edition: String = "",                   // 4
    val language: String = "",                  // 5
    val publisher: String = "",                 // 6
    val year: String = "",                      // 7
    val toBeTranslated: Boolean = false,        // 8
    val stakeholder: String = "",               // 9
    val place: String = "",                     // 10 (mandatory only when Stakeholder is filled)
    val accessionNo: String = "",               // 11
    val eLibrary: String = "",                  // 12
    val translationAvailable: Boolean = false,  // 13
    val translationLanguages: String = "",      // 14 (shown only when 13 is checked)
)

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val prefix: String = "",
    val name: String = "",
    val sex: String = "",          // "", MALE, FEMALE, TRANSGENDER
    val designation: String = "",
    val institution: String = "",
    val address: String = "",
    val place: String = "",
    val photoFile: String = "",    // file name inside the app's private storage, or ""
) {
    val isEmpty: Boolean
        get() = listOf(prefix, name, sex, designation, institution, address, place, photoFile).all { it.isBlank() }
}
