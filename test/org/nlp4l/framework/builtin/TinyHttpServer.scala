/*
 * Copyright 2016 org.NLP4L
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nlp4l.framework.builtin

import java.io.{PrintWriter, OutputStream}
import java.net.{HttpURLConnection, InetSocketAddress, ServerSocket, Socket}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

class TinyHttpServer(val port: Int){

  val serverSocket = new ServerSocket()
  serverSocket.setReuseAddress(true)
  serverSocket.bind(new InetSocketAddress(port))

  def shutdown(): Unit = {
    if(serverSocket != null){
      serverSocket.close()
    }
  }

  def accept(): Option[Socket] = {
    try {
      Some(serverSocket.accept())
    }
    catch {
      case e: Exception => {
        println("done")
        None
      }
    }
  }

  def response(stat: Int, msg: String, out: OutputStream): Unit = {
    val pw = new PrintWriter(out)
    pw.print("HTTP/1.1 ")
    pw.print(stat)
    pw.print(" ")
    pw.print(msg)
    pw.print("\r\n\r\n")
    pw.flush()
  }

  def service(): Unit = {
    var loop = true
    while(loop){
      val sock = accept()
      if(sock != None){
        val socket = sock.get
        try{
          val req = new Request(socket)
          if(req.path == "/shutdown"){
            println("***** /shutdown *****")
            response(200, "OK", socket.getOutputStream())
            loop = false
          }
          else{
            println(s"*****${new String(req.content)}*****")
            response(req.status, "OK", socket.getOutputStream())
          }
        }
        catch {
          case e: Exception => {
            e.printStackTrace()
            response(500, e.getMessage, socket.getOutputStream())
            loop = false
          }
        }
        finally {
          if(sock != None)
            sock.get.close()
        }
      }
    }
  }
}

object TinyHttpServer {
  def main(args: Array[String]): Unit = {
    val server = new TinyHttpServer(9010)
    server.service()
    server.shutdown()
  }
}

class Request(val sock: Socket){

  val is = sock.getInputStream
  val command = "^(\\w+)\\s+(.+?)\\s+HTTP/([\\d.]+)$".r
  val (status, header, content) = process()
  println(s"header = ${new String(header)}")
  println(s"content = ${new String(content)}")
  val (method, path, version) = {
    val hstr = new String(header)
    hstr.split("\r\n")(0) match {
      case command(a,b,c) => (a,b,c)
      case _ => (null, null, null)
    }
  }

  def process(): (Int, Array[Byte], Array[Byte]) = {
    val hbuf = ArrayBuffer.empty[Byte]
    val cbuf = ArrayBuffer.empty[Byte]
    @tailrec
    def readHeader(): (Int, Array[Byte], Array[Byte]) = {
      val c: Int = is.read()
      if(c < 0){
        println("end of input stream was found")
        (HttpURLConnection.HTTP_BAD_REQUEST, hbuf.toArray, cbuf.toArray)
      }
      else {
        hbuf.append(c.asInstanceOf[Byte])
        val i = hbuf.size
        if(i > 3
          && hbuf(i - 4) == '\r' && hbuf(i - 3) == '\n'
          && hbuf(i - 2) == '\r' && hbuf(i - 1) == '\n') {
          val clen = contentLength(hbuf.toArray)
          @tailrec
          def readContent(len: Int): (Int, Array[Byte]) = {
            if(len == 0){
              (HttpURLConnection.HTTP_OK, cbuf.toArray)
            }
            else{
              val c: Int = is.read()
              if(c < 0){
                println("end of input stream was found")
                (HttpURLConnection.HTTP_BAD_REQUEST, cbuf.toArray)
              }
              else {
                cbuf.append(c.asInstanceOf[Byte])
                readContent(len - 1)
              }
            }
          }
          readContent(clen)
          (HttpURLConnection.HTTP_OK, hbuf.toArray, cbuf.toArray)
        }
        else{
          readHeader()
        }
      }
    }
    readHeader()
  }

  def contentLength(metadata: Array[Byte]): Int = {
    val metestr = new String(metadata)
    val contlen = metestr.split("\r\n").filter(a => a.toLowerCase().startsWith("content-length:"))
    if(contlen.length == 0) 0
    else{
      contlen.head.split(":")(1).trim.toInt
    }
  }
}

  /*

  def response(req: Request, out: OutputStream): Unit = {
    req.method match {
      case "GET" => {
        println("***GET***")
      }
      case "POST" => {
        println("***POST***")
      }
      case a => throw new IllegalArgumentException(s"unknow method: ${req.method} ${HttpURLConnection.HTTP_BAD_METHOD}")
    }
  }

  void response(File f, OutputStream out) throws IOException {
    responseSuccess((int)f.length(), "text/html", out);
    BufferedInputStream bi = new BufferedInputStream(new FileInputStream(f));
    try {
      for (int c = bi.read(); c >= 0; c = bi.read()) {
      out.write(c);
    }
    } finally {
      bi.close();
    }
  }

  void responseSuccess(int len, String type, OutputStream out) throws IOException {
    PrintWriter prn = new PrintWriter(out);
    prn.print("HTTP/1.1 200 OK\r\n");
    prn.print("Connection: close\r\n");
    prn.print("Content-Length: ");
    prn.print(len);
    prn.print("\r\n");
    prn.print("Content-Type: ");
    prn.print(type);
    prn.print("\r\n\r\n");
    prn.flush();
  }
*/
