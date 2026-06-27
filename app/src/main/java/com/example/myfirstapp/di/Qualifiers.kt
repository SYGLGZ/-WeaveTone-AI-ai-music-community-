package com.example.myfirstapp.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendApi
