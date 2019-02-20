package com.microsoft.kusto.spark.datasink

import java.util

import com.microsoft.azure.kusto.data.Client
import com.microsoft.kusto.spark.datasource.{KustoAuthentication, KustoCoordinates}
import com.microsoft.kusto.spark.utils.CslCommandsGenerator._
import com.microsoft.kusto.spark.utils.KustoClient
import org.joda.time.{DateTime, DateTimeZone, Period}

object TempStorageCache{

  var roundRubinIdx = 0
  var cluster = ""
  var storages = new util.ArrayList[String]
  var dmClient: Client = _
  var lastRefresh: DateTime = new DateTime(DateTimeZone.UTC)

  val storageExpiryMinutes = 120

  def getNewTempBlobReference(authentication: KustoAuthentication, clusterAlias: String): String = {
    getNextUri(authentication, clusterAlias)
  }

  private def getNextUri(authentication: KustoAuthentication, clusterAlias: String): String = {
    // Refresh if 120 minutes have passed since last refresh for this cluster as SAS should be valid for at least 120 minutes
    if(storages.size() == 0 || cluster != clusterAlias || new Period(new DateTime(DateTimeZone.UTC), lastRefresh).getMinutes > storageExpiryMinutes){
      dmClient = KustoClient.getAdmin(authentication, clusterAlias, isAdminCluster = true)
      cluster = clusterAlias

      lastRefresh = new DateTime(DateTimeZone.UTC)

      val res = dmClient.execute(generateCreateTmpStorageCommand())
      storages = res.getValues.get(0)
    }

    roundRubinIdx = (roundRubinIdx + 1) % storages.size
    storages.get(roundRubinIdx)
  }

}