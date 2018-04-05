package edu.unh.cs.ai.realtimesearch.visualizer

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.experimental.runBlocking
import java.net.URL


/**
 * @author Bence Cserna (bence@cserna.net)
 */

fun main(args: Array<String>) {

    val client = HttpClient(CIO)
//    val post = khttp.post(
//            url = "http://localhost:8080/workspace?operation=updateGraph",
//            params = mapOf("operation" to "updateGraph"),
//            data = "{\"an\":{\"X2\":{\"label\":\"Streaming Node X2\"}}}"
//    )
//    println(post.content)
//    println(post.text)

//    val (request, response, result1) = Fuel
//            .post("http://localhost:8081/workspace1", parameters = listOf("operation" to "updateGraph"))
//            .body(body = "{\"an\":{\"X2\":{\"label\":\"Streaming Node X2\"}}}", charset = Charset.forName("UTF-8"))
//            .response()
//
//    println("request $request")
//    println("response $response")
//    println("error $result1")

//    return
    runBlocking {
        //                client.post<String>(
////                port = 9000,
////                host = "127.0.0.1",
////                path = "workspace1?operation=updateGraph",
////                body = "{\"an\":{\"X\":{\"label\":\"Streaming Node X\"}}}",
////
////        )
//
//        val parameters = ParametersBuilder()
//        parameters.append("operation", "updateGraph")
//
//        val urlBuilder = URLBuilder(port = 8081, encodedPath = "workspace1", parameters = parameters)
//
//        val request = HttpRequestData(
//                url = urlBuilder.build(),
//                method = HttpMethod.Post,
//                headers = HeadersBuilder().build(),
//                body = "{\"an\":{\"X\":{\"label\":\"Streaming Node X\"}}}",
//                executionContext = CompletableDeferred<Unit>()
//        )
//
//
//        HttpClientCall.create()
//
//
        val result = client.post<String> {
            url(URL("http://localhost:8080/workspace1?operation=updateGraph"))

            accept(ContentType.Any)
//            contentType(ContentType.Text.Plain)
            contentType(ContentType.Application.FormUrlEncoded)
//            body = URLEncoder.encode("{\"an\":{\"X11\":{\"label\":\"Streaming Node X11\"}}}", Charsets.UTF_8.toString())
            body = "{\"an\":{\"X12\":{\"label\":\"Streaming Node X12\"}}}"
//
////            body = "{\"ae\":{\"BC\":{\"source\":\"B\",\"target\":\"C\",\"directed\":false}}}"
////            body = "{\"an\":{\"A\":{\"label\":\"Streaming Node A\"}}}\\r\n" +
////                    "{\"an\":{\"B\":{\"label\":\"Streaming Node B\"}}}\\r\n" +
////                    "{\"an\":{\"C\":{\"label\":\"Streaming Node C\"}}}\\r\n" +
////                    "{\"ae\":{\"AB\":{\"source\":\"A\",\"target\":\"B\",\"directed\":false}}}\\r\n" +
////                    "{\"ae\":{\"BC\":{\"source\":\"B\",\"target\":\"C\",\"directed\":false}}}\\r\n" +
////                    "{\"ae\":{\"CA\":{\"source\":\"C\",\"target\":\"A\",\"directed\":false}}}"
        }
//
        println(result)
    }

    client.close()
}