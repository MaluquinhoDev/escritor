package br.com.maluquinho.escritor

import br.com.maluquinho.escritor.data.StoryChapter
import br.com.maluquinho.escritor.data.StoryProject
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private fun xmlSafe(value:String)=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
fun projectAsMarkdown(project:StoryProject,chapters:List<StoryChapter>):String=buildString{
 append("# ").append(project.title).append("\n\n").append("**Formato:** ").append(project.format).append("\n\n")
 if(project.synopsis.isNotBlank())append("## Sinopse\n\n").append(project.synopsis).append("\n\n")
 chapters.forEachIndexed{index,c->append("## Capítulo ").append(index+1).append(" — ").append(c.title).append("\n\n").append(c.content).append("\n\n")}
}
fun projectAsDocx(project:StoryProject,chapters:List<StoryChapter>):ByteArray{
 val p=buildString{
  append("<w:p><w:pPr><w:pStyle w:val=\"Title\"/></w:pPr><w:r><w:t>").append(xmlSafe(project.title)).append("</w:t></w:r></w:p>")
  if(project.synopsis.isNotBlank())append("<w:p><w:r><w:t>").append(xmlSafe("Sinopse: "+project.synopsis)).append("</w:t></w:r></w:p>")
  chapters.forEachIndexed{index,c->
   append("<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:t>").append(xmlSafe("Capítulo "+(index+1)+" — "+c.title)).append("</w:t></w:r></w:p>")
   c.content.split("\n").forEach{line->append("<w:p><w:r><w:t xml:space=\"preserve\">").append(xmlSafe(line)).append("</w:t></w:r></w:p>")}
  }
 }
 val doc="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+p+"<w:sectPr/></w:body></w:document>"
 return zipOf(listOf(
 "[Content_Types].xml" to "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
 "_rels/.rels" to "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>",
 "word/document.xml" to doc))
}
fun projectAsEpub(project:StoryProject,chapters:List<StoryChapter>):ByteArray{
 val body=buildString{
  append("<h1>").append(xmlSafe(project.title)).append("</h1>")
  if(project.synopsis.isNotBlank())append("<h2>Sinopse</h2><p>").append(xmlSafe(project.synopsis)).append("</p>")
  chapters.forEachIndexed{index,c->append("<h2>Capítulo ").append(index+1).append(" — ").append(xmlSafe(c.title)).append("</h2>");c.content.split("\n").forEach{append("<p>").append(xmlSafe(it)).append("</p>")}}
 }
 val xhtml="<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>"+xmlSafe(project.title)+"</title><meta charset=\"UTF-8\"/></head><body>"+body+"</body></html>"
 val container="<?xml version=\"1.0\"?><container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>"
 val opf="<?xml version=\"1.0\" encoding=\"UTF-8\"?><package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"bookid\"><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:identifier id=\"bookid\">urn:uuid:escritor-"+project.id+"</dc:identifier><dc:title>"+xmlSafe(project.title)+"</dc:title><dc:language>pt-BR</dc:language></metadata><manifest><item id=\"chapter\" href=\"chapter.xhtml\" media-type=\"application/xhtml+xml\"/><item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/></manifest><spine><itemref idref=\"chapter\"/></spine></package>"
 val nav="<?xml version=\"1.0\"?><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\"><head><title>Sumário</title></head><body><nav epub:type=\"toc\"><h1>Sumário</h1><ol><li><a href=\"chapter.xhtml\">"+xmlSafe(project.title)+"</a></li></ol></nav></body></html>"
 val out=ByteArrayOutputStream()
 ZipOutputStream(out).use{zip->
  val mime="application/epub+zip".toByteArray(Charsets.US_ASCII);val crc=CRC32().apply{update(mime)}
  val first=ZipEntry("mimetype").apply{method=ZipEntry.STORED;size=mime.size.toLong();compressedSize=mime.size.toLong();this.crc=crc.value}
  zip.putNextEntry(first);zip.write(mime);zip.closeEntry()
  listOf("META-INF/container.xml" to container,"OEBPS/content.opf" to opf,"OEBPS/nav.xhtml" to nav,"OEBPS/chapter.xhtml" to xhtml).forEach{(n,v)->zip.putNextEntry(ZipEntry(n));zip.write(v.toByteArray(Charsets.UTF_8));zip.closeEntry()}
 }
 return out.toByteArray()
}
fun projectBackupZip(project:StoryProject,chapters:List<StoryChapter>):ByteArray=zipOf(listOf(
 "README.txt" to "Backup textual do projeto Escritor. Importação automática ainda não incluída.",
 "project.md" to projectAsMarkdown(project,chapters),
 "project-info.txt" to ("Título: "+project.title+"\nFormato: "+project.format+"\nID local: "+project.id)
))
private fun zipOf(entries:List<Pair<String,String>>):ByteArray{
 val out=ByteArrayOutputStream()
 ZipOutputStream(out).use{zip->entries.forEach{(name,value)->zip.putNextEntry(ZipEntry(name));zip.write(value.toByteArray(Charsets.UTF_8));zip.closeEntry()}}
 return out.toByteArray()
}
