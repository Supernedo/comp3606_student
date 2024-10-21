package com.example.comp3606_a1_student.network

import com.example.comp3606_a1_student.models.ContentModel

interface NetworkMessageInterface {
    fun onContent(content: ContentModel)
}