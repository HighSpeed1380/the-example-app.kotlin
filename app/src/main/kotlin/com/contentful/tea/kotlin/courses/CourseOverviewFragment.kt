package com.contentful.tea.kotlin.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.contentful.tea.kotlin.R
import com.contentful.tea.kotlin.contentful.Course
import com.contentful.tea.kotlin.dependencies.Dependencies
import com.contentful.tea.kotlin.dependencies.DependenciesProvider
import kotlinx.android.synthetic.main.fragment_course_overview.*
import kotlinx.android.synthetic.main.item_lesson.view.*

class CourseOverviewFragment : Fragment() {
    private var courseSlug: String? = null
    private var firstLessonSlug: String? = null

    private lateinit var dependencies: Dependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseSlug = CourseOverviewFragmentArgs.fromBundle(arguments).courseSlug
        }

        if (activity !is DependenciesProvider) {
            throw IllegalStateException("Activity must implement Dependency provider.")
        }

        dependencies = (activity as DependenciesProvider).dependencies()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_course_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        overview_next.setOnClickListener { onNextButtonClicked() }

        courseSlug?.let {
            dependencies
                .contentful
                .fetchCourseBySlug(courseSlug!!) { course ->
                    activity?.runOnUiThread {
                        updateData(course)
                    }
                }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateData(course: Course) {
        firstLessonSlug = if (course.lessons.isNotEmpty()) course.lessons.first().slug else null
        val parser = dependencies.markdown

        overview_title.text = parser.parse(course.title)
        overview_description.text = parser.parse(course.description)
        overview_duration.text = parser.parse(
            getString(
                R.string.lesson_duration,
                course.duration,
                course.skillLevel
            )
        )

        val inflater = LayoutInflater.from(context)
        course.lessons.forEach { lesson ->
            val index = course.lessons.indexOf(lesson)
            inflater
                .inflate(R.layout.item_lesson, overview_container, false)
                .apply {
                    this.lesson_item_title.text = parser.parse(lesson.title)
                    this.lesson_item_description.text = parser.parse(
                        getString(R.string.lesson_number, index + 1)
                    )
                    setOnClickListener {
                        lessonClicked(lesson.slug)
                    }

                    overview_container.addView(this)
                }
        }
    }

    private fun lessonClicked(lessonSlug: String) {
        val navController = NavHostFragment.findNavController(this)
        val action = CourseOverviewFragmentDirections.openLesson(courseSlug, lessonSlug)
        navController.navigate(action)
    }

    private fun onNextButtonClicked() = firstLessonSlug?.let { lessonClicked(it) }
}
