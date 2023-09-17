package com.arshadshah.nimaz.utils.api

import com.apollographql.apollo3.ApolloClient
import com.arshadshah.nimaz.constants.AppConstants

object ApolloClientUtil
{
	private val apolloClient : ApolloClient = ApolloClient.Builder()
			.serverUrl(AppConstants.BASE_URL)
			.build()
	fun getApolloClient() : ApolloClient = apolloClient
}