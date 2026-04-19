package com.tricare.manuals.di

import android.content.Context
import androidx.room.Room
import com.tricare.manuals.data.db.AppDatabase
import com.tricare.manuals.data.db.BookmarkDao
import com.tricare.manuals.data.db.HighlightDao
import com.tricare.manuals.data.db.ManualDao
import com.tricare.manuals.data.db.SectionDao
import com.tricare.manuals.data.network.TocParser
import com.tricare.manuals.data.network.TricareWebClient
import com.tricare.manuals.data.network.VersionChecker
import com.tricare.manuals.data.repository.ManualRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tricare_manuals.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideManualDao(db: AppDatabase): ManualDao = db.manualDao()

    @Provides
    @Singleton
    fun provideSectionDao(db: AppDatabase): SectionDao = db.sectionDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    @Singleton
    fun provideHighlightDao(db: AppDatabase): HighlightDao = db.highlightDao()

    @Provides
    @Singleton
    fun provideTricareWebClient(): TricareWebClient = TricareWebClient()

    @Provides
    @Singleton
    fun provideVersionChecker(webClient: TricareWebClient): VersionChecker =
        VersionChecker(webClient)

    @Provides
    @Singleton
    fun provideTocParser(): TocParser = TocParser()

    @Provides
    @Singleton
    fun provideManualRepository(
        manualDao: ManualDao,
        sectionDao: SectionDao,
        bookmarkDao: BookmarkDao,
        webClient: TricareWebClient,
        versionChecker: VersionChecker
    ): ManualRepository = ManualRepository(manualDao, sectionDao, bookmarkDao, webClient, versionChecker)
}
