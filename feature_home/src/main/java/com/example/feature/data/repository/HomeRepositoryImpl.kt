package com.example.feature.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.bumptech.glide.load.HttpException
import com.example.core.utils.Resource
import com.example.feature.data.local.cache.HomeDatabase
import com.example.feature.data.local.settings.PreferencesManager
import com.example.feature.data.remote.HomeApi
import com.example.feature.domain.model.DomainDataSource
import com.example.feature.domain.repository.HomeRepository
import com.example.feature.presentation.home.model.SortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class HomeRepositoryImpl(
    private val db: HomeDatabase,
    private val api: HomeApi,
    private val sharedPref: PreferencesManager
) : HomeRepository {

    private val dao = db.dao

    override fun fetchData(
        department: String,
        sortType: SortType,
        searchQuery: String,
        fetchFromRemote: Boolean,
    ): Flow<Resource<List<DomainDataSource>>> {
        return flow {
            emit(Resource.Loading(isLoading = true))

            if (!fetchFromRemote) {
                val cache = dao.fetchCache(department, sortType, searchQuery)
                emit(Resource.Success(cache.map { it.toDomainDataSource() }))
                emit(Resource.Loading(isLoading = false))
                return@flow
            }

            val response = try {
                api.fetchCloudDataSource()
            } catch (e: HttpException) {
                emit(Resource.Error(error = e))
                null
            } catch (e: IOException) {
                emit(Resource.Error(error = e))
                null
            }

            response?.let { data ->
                db.withTransaction {
                    dao.clearCache()
                    dao.insertCache(data.items.map { it.toCacheDataSource() })
                }

                emit(
                    Resource.Success(
                        dao.fetchCache(department, sortType, searchQuery)
                            .map { it.toDomainDataSource() })
                )
                emit(Resource.Loading(isLoading = false))
            }
        }
    }


    override fun fetchSortType() = sharedPref.fetchSortType()

    override fun updateSortType(sortType: SortType) = sharedPref.updateSortType(sortType)

}