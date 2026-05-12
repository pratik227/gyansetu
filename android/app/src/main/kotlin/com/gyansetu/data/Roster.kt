package com.gyansetu.data

/** A student in Teacher Mode's multi-kid roster. The id is the persisted key
 *  in DataStore (Settings.ACTIVE_STUDENT). */
data class Student(
    val id: String,
    val name: String,
    val gu: String,
    val grade: String,
)

/** Single source of truth for the roster — used by HomeScreen for the
 *  greeting + avatar and by TeacherModeScreen for the student switcher. */
val ROSTER: List<Student> = listOf(
    Student("kiran", "Kiran", "કિરણ",   "Std 3"),
    Student("aarav", "Aarav", "આરવ",    "Std 2"),
    Student("priya", "Priya", "પ્રિયા", "Std 4"),
    Student("dev",   "Dev",   "દેવ",    "Std 5"),
)

fun activeStudent(activeId: String): Student =
    ROSTER.firstOrNull { it.id == activeId } ?: ROSTER.first()
