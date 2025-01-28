package com.arshadshah.nimaz.modules

import android.content.Context
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.arshadshah.nimaz.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpacesModule {

    @Provides
    @Singleton
    fun provideAWSCredentials(): AWSCredentialsProvider {
        return StaticCredentialsProvider(
            BasicAWSCredentials(
                BuildConfig.DO_ACCESS_KEY,
                BuildConfig.DO_SECRET_KEY
            )
        )
    }

    @Provides
    @Singleton
    fun provideS3Client(credentialsProvider: AWSCredentialsProvider): AmazonS3Client {
        return AmazonS3Client(credentialsProvider, Region.getRegion("us-east-1")).apply {
            endpoint = SpaceRegion.NYC.endpoint()
        }
    }

    @Provides
    @Singleton
    fun provideTransferUtility(
        @ApplicationContext context: Context,
        s3Client: AmazonS3Client
    ): TransferUtility {
        return TransferUtility.builder()
            .s3Client(s3Client)
            .context(context)
            .build()
    }
}

enum class SpaceRegion {
    NYC;

    fun endpoint(): String = when (this) {
        NYC -> "https://nyc3.digitaloceanspaces.com"
    }
}