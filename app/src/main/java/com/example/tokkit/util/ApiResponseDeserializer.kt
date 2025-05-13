//package com.example.tokkit.util
//
//import com.example.tokkit.data.remote.model.ApiResponse
//import com.google.gson.*
//import java.lang.reflect.ParameterizedType
//import java.lang.reflect.Type
//import android.util.Log
//
//class ApiResponseDeserializer<T> : JsonDeserializer<ApiResponse<T>> {
//    override fun deserialize(
//        json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext
//    ): ApiResponse<T> {
//        Log.d("Deserializer", "ApiResponseDeserializer invoked")
//
//        val jsonObject = json!!.asJsonObject
//
//        val isSuccess = jsonObject["isSuccess"].asBoolean
//        val code = jsonObject["code"].asString
//        val message = jsonObject["message"].asString
//
//        val resultType = (typeOfT as? ParameterizedType)?.actualTypeArguments?.get(0)
//        val result: T? = if (resultType != null && jsonObject["result"] != null && !jsonObject["result"].isJsonNull) {
//            context.deserialize(jsonObject["result"], resultType)
//        } else null
//
//        return ApiResponse(isSuccess, code, message, result!!)
//    }
//}
