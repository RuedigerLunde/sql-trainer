<?xml version="1.0"?>
<!--exercise-set.xsl-->
<xsl:stylesheet version="1.0" 
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   <xsl:template match="exercise-set">
      <html>
         <head>
            <title>
               Übungsblatt <xsl:value-of select="@id"/>
            </title>
        </head>
        <body>
          <center>
        	<h2><xsl:value-of select="@course"/><br>Übungsblatt <xsl:value-of select="@id"/></br></h2>
            <h3><xsl:value-of select="@lecturer"/></h3>
          </center>
          <p><i>Datenbank: <xsl:value-of select="@db"/></i>
          <br/><i>Bearbeitung: <xsl:value-of select="@editor"/></i>
          <br/><i>Letzte Änderung: <xsl:value-of select="@lastEdit"/></i></p>
          <xsl:apply-templates/>
        </body>
      </html>
   </xsl:template>
   
   <xsl:template match="intro">
     <p><xsl:apply-templates/></p>
   </xsl:template>
   
   <xsl:template match="exercise">
     <h3>Aufgabe <xsl:value-of select="@id"/>
     <xsl:apply-templates select="@theme"/>
     </h3>
     <xsl:apply-templates select="question"/>
     <xsl:apply-templates select="answer"/>
   </xsl:template>
   
   <xsl:template match="@theme">
     (<xsl:value-of select="."/>)
   </xsl:template>
   
   <xsl:template match="answer">
     <pre><xsl:value-of select="."/></pre>
   </xsl:template>
   
   <!-- trivial html markup -->
   
   <xsl:template match="p">
     <p><xsl:apply-templates/></p>
   </xsl:template>
   
   <xsl:template match="b">
     <b><xsl:apply-templates/></b>
   </xsl:template>
   
   <xsl:template match="i">
     <i><xsl:apply-templates/></i>
   </xsl:template>
   
   <xsl:template match="tt">
     <tt><xsl:apply-templates/></tt>
   </xsl:template>
   
   <xsl:template match="ul">
     <ul><xsl:apply-templates/></ul>
   </xsl:template>
   
   <xsl:template match="li">
     <li><xsl:apply-templates/></li>
   </xsl:template>
   
   <xsl:template match="pre">
     <pre><xsl:apply-templates/></pre>
   </xsl:template>
   
</xsl:stylesheet>
