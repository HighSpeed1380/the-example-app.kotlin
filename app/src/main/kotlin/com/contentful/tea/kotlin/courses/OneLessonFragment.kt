package com.contentful.tea.kotlin.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.contentful.tea.kotlin.R
import com.contentful.tea.kotlin.contentful.Course
import com.contentful.tea.kotlin.contentful.LessonModule
import com.contentful.tea.kotlin.dependencies.Dependencies
import com.contentful.tea.kotlin.dependencies.DependenciesProvider
import com.contentful.tea.kotlin.extensions.saveToClipboard
import com.contentful.tea.kotlin.extensions.setImageResourceFromUrl
import com.contentful.tea.kotlin.extensions.showError
import com.contentful.tea.kotlin.extensions.toast
import kotlinx.android.synthetic.main.fragment_lesson.*
import kotlinx.android.synthetic.main.lesson_module_code.view.*
import kotlinx.android.synthetic.main.lesson_module_copy.view.*
import kotlinx.android.synthetic.main.lesson_module_image.view.*

class OneLessonFragment : Fragment() {
    private var courseSlug: String? = null
    private var lessonSlug: String? = null

    private lateinit var dependencies: Dependencies

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.apply {
            courseSlug = OneLessonFragmentArgs.fromBundle(arguments).courseSlug
            lessonSlug = OneLessonFragmentArgs.fromBundle(arguments).lessonSlug
        }

        if (activity !is DependenciesProvider) {
            throw IllegalStateException("Activity must implement Dependency provider.")
        }

        dependencies = (activity as DependenciesProvider).dependencies()

        return inflater.inflate(R.layout.fragment_lesson, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseSlug?.apply {
            dependencies
                .contentful
                .fetchCourseBySlug(
                    this,
                    ::lessonNotFound
                ) { course ->
                    activity?.runOnUiThread {
                        updateData(course)
                    }
                }
        }
    }

    private fun updateData(course: Course) {
        val selectedLesson = course.lessons.firstOrNull { it.slug == lessonSlug }
        if (selectedLesson == null) {
            lessonNotFound(
                IllegalStateException("""Lesson "$lessonSlug" in "$courseSlug" not found.""")
            )
        } else {

            val nextIndex = course.lessons.indexOf(selectedLesson) + 1
            if (nextIndex >= course.lessons.lastIndex) {
                lesson_next_button?.hide()
            } else {
                lesson_next_button?.setOnClickListener {
                    nextLessonClicked(course.lessons[nextIndex].slug)
                }
            }

            selectedLesson.modules.forEach {
                addModule(it)
            }
        }
    }

    private fun addModule(
        module: LessonModule,
        inflater: LayoutInflater = LayoutInflater.from(context)
    ) = when (module) {
        is LessonModule.CodeSnippet -> {
            lesson_module_container.addView(createCodeView(inflater, module))
        }
        is LessonModule.Image -> {
            lesson_module_container.addView(createImageView(inflater, module))
        }
        is LessonModule.Copy -> {
            lesson_module_container.addView(createCopyView(inflater, module))
        }
    }

    private fun createCodeView(inflater: LayoutInflater, module: LessonModule.CodeSnippet): View {
        val codeView = inflater.inflate(R.layout.lesson_module_code, lesson_module_container, false)

        val languageAdapter = ArrayAdapter<String>(
            activity,
            R.layout.item_language_spinner,
            R.id.language_item_name,
            resources.getStringArray(R.array.code_languages)
        )
        languageAdapter.setDropDownViewResource(R.layout.item_language_spinner)

        codeView.module_code_language_selector.setSelection(0)
        codeView.module_code_language_selector.adapter = languageAdapter
        codeView.module_code_language_selector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    codeView.module_code_source.text =
                        getString(R.string.module_code_select_language)
                }

                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val language = resources.getStringArray(R.array.code_languages)[position]
                    codeView.module_code_source.text = sourceCodeFromLanguageIndex(module, language)
                }
            }

        codeView.module_code_source.text = module.javaAndroid
        codeView.module_code_source.setOnClickListener {
            activity?.saveToClipboard(
                codeView.module_code_language_selector.selectedItem.toString(),
                codeView.module_code_source.text.toString()
            )
            activity?.toast(getString(R.string.module_code_source_copied))
        }
        return codeView
    }

    private fun createImageView(inflater: LayoutInflater, module: LessonModule.Image): View {
        val view = inflater.inflate(R.layout.lesson_module_image, lesson_module_container, false)
        view.module_image_caption.text = dependencies.markdown.parse(module.caption)
        view.module_image_image.setImageResourceFromUrl(
            module.image,
            R.mipmap.ic_launcher_foreground
        )
        return view
    }

    private fun createCopyView(inflater: LayoutInflater, module: LessonModule.Copy): View {
        val view = inflater.inflate(R.layout.lesson_module_copy, lesson_module_container, false)
        view.module_copy_text.text = dependencies.markdown.parse(module.copy)
        return view
    }

    private fun sourceCodeFromLanguageIndex(
        codeModule: LessonModule.CodeSnippet,
        language: String
    ): CharSequence = when (language.toLowerCase()) {
        "curl" -> codeModule.curl
        "dotnet" -> codeModule.dotNet
        "javascript" -> codeModule.javascript
        "java" -> codeModule.java
        "javaandroid" -> codeModule.javaAndroid
        "php" -> codeModule.php
        "python" -> codeModule.python
        "ruby" -> codeModule.ruby
        "swift" -> codeModule.swift
        else -> codeModule.javaAndroid
    }

    private fun nextLessonClicked(lessonSlug: String) {
        val navController = NavHostFragment.findNavController(this)
        val action = CourseOverviewFragmentDirections.openLesson(courseSlug, lessonSlug)
        navController.navigate(action)
    }

    private fun lessonNotFound(throwable: Throwable) {
    }
}
