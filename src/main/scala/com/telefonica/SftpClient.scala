package com.telefonica

import com.jcraft.jsch.SftpATTRS
import com.jcraft.jsch.JSch
import com.jcraft.jsch.ChannelSftp
import java.io.File
import scala.collection.mutable.ListBuffer

class SftpClient {

  
  def listFiles(username: String, password: String, host: String, port: Int, path: String){
    val jsch = new JSch()
    try{
      val jschSession = jsch.getSession(username, host, port)
      val config = new java.util.Properties()
      config.put("StrictHostKeyChecking", "no")
      jschSession.setConfig(config)
      jschSession.setPassword(password)
      jschSession.connect()
      if(jschSession.isConnected()){
        val sftp = (jschSession.openChannel("sftp")).asInstanceOf[ChannelSftp]
        sftp.connect()
        if(sftp.isConnected()){
          println("Arquivos do Diretòrio:")
          val lista = null
          listDirectory(sftp, path, lista)
        	sftp.disconnect()
        }
        jschSession.disconnect()
      } else {
       throw new Exception(s"Erro ao tentar conectar no SFTP: $username@$host") 
      }
    } catch {
      case e: Throwable =>
      e.printStackTrace()
    }
  }
  
  def listDirectory(sftp:ChannelSftp, path:String, lista:List[File]){
    val lista:List[File] = (sftp.ls(path))asInstanceOf; // pasa directorio, guardo su contenido
    var listBuf = new ListBuffer[File]()
      for(l <- lista){
        if (!l.isDirectory()){
          val file = new File(path + "/" + l.getName)
          listBuf += file
        } else if(!l.getName.equals(".") && !l.getName.equals("..")){
          listDirectory(sftp, path+"/"+l.getName, listBuf.toList) 
        }
      }
  }
  
  def downloadFromServer(username: String,
      password: String, host: String, port: Int, path:String, filename:String, diretorio:String){
    val jsch = new JSch()
    try{
      val jschSession = jsch.getSession(username, host, port)
      val config = new java.util.Properties()
      config.put("StrictHostKeyChecking", "no")
      jschSession.setConfig(config)
      jschSession.setPassword(password)
      jschSession.connect()
      if(jschSession.isConnected()){
        val sftp = (jschSession.openChannel("sftp")).asInstanceOf[ChannelSftp]
        sftp.connect()
        if(sftp.isConnected()){
          
//          So um arquivo
//          sftp.get(filename, diretorio)
          
//          Todos os arquivos
//          sftp.get("*", diretorio)
          println("\nTransferência concluída")
          
        	sftp.disconnect()
        }
        jschSession.disconnect()
      } else {
       throw new Exception(s"Erro ao tentar conectar no SFTP: $username@$host") 
      }
    } catch {
      case e: Throwable =>
      e.printStackTrace()
    }
  }
  
  def uploadLocalFile(username: String,
      password: String, host: String, port: Int, filename:String){
    val jsch = new JSch()
    try{
      val jschSession = jsch.getSession(username, host, port)
      val config = new java.util.Properties()
      config.put("StrictHostKeyChecking", "no")
      jschSession.setConfig(config)
      jschSession.setPassword(password)
      jschSession.connect()
      if(jschSession.isConnected()){
        val sftp = (jschSession.openChannel("sftp")).asInstanceOf[ChannelSftp]
        sftp.connect()
        if(sftp.isConnected()){
//          so um arquivo
          sftp.put(filename)
          
          println("\nTransferência concluída")
          
        	sftp.disconnect()
        }
        jschSession.disconnect()
      } else {
       throw new Exception(s"Erro ao tentar conectar no SFTP: $username@$host") 
      }
    } catch {
      case e: Throwable =>
      e.printStackTrace()
    }
  }
  
}