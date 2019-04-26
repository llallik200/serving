package org.apache.spark.ml.clustering

import java.util

import org.apache.spark.ml.data.{SCol, SDFrame, SRow, UDF}
import org.apache.spark.ml.linalg.{Vector, VectorUDT}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.transformer.ServingModel
import org.apache.spark.ml.util.SchemaUtils
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}

class BisectingKMeansServingModel(stage: BisectingKMeansModel)
  extends ServingModel[BisectingKMeansServingModel] with BisectingKMeansParams {

  override def copy(extra: ParamMap): BisectingKMeansServingModel = {
    new BisectingKMeansServingModel(stage.copy(extra))
  }

  override def transform(dataset: SDFrame): SDFrame = {
    transformSchema(dataset.schema, logging = true)
    val predictUDF = UDF.make[Int, Vector](stage.predict, false)
    dataset.withColum(predictUDF(stage.getPredictionCol, SCol(stage.getFeaturesCol)))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchemaImpl(schema)
  }

  /**
    * Validates and transforms the input schema.
    * @param schema input schema
    * @return output schema
    */
  def validateAndTransformSchemaImpl(schema: StructType): StructType = {
    SchemaUtils.checkColumnType(schema, stage.getFeaturesCol, new VectorUDT)
    SchemaUtils.appendColumn(schema, stage.getPredictionCol, IntegerType)
  }

  override val uid: String = stage.uid

  override def prepareData(rows: Array[SRow]): SDFrame = {
    if (stage.isDefined(stage.featuresCol)) {
      val schema = new StructType().add(new StructField(stage.getFeaturesCol, new VectorUDT, true))
      new SDFrame(rows)(schema)
    } else {
      throw new Exception (s"featuresCol of ${stage} is not defined!")
    }
  }

  override def prepareData(feature: util.Map[String, _]): SDFrame = {
    if (stage.isDefined(stage.featuresCol)) {
      val featureName = feature.keySet.toArray
      if (!featureName.contains(stage.getFeaturesCol)) {
        throw new Exception (s"the ${stage.getFeaturesCol} is not included in the input col(s)")
      } else if (!feature.get(stage.getFeaturesCol).isInstanceOf[Vector]) {
        throw new Exception (s"the type of col ${stage.getFeaturesCol} is not Vector")
      } else {
        val schema = new StructType().add(new StructField(stage.getFeaturesCol, new VectorUDT, true))
        val rows =  Array(new SRow(Array(feature.get(stage.getFeaturesCol))))
        new SDFrame(rows)(schema)
      }
    } else {
      throw new Exception (s"featuresCol of ${stage} is not defined!")
    }
  }
}

object BisectingKMeansServingModel {
  def apply(stage: BisectingKMeansModel): BisectingKMeansServingModel =
    new BisectingKMeansServingModel(stage)
}
