<?xml version="1.0" encoding="ISO-8859-1"?>
 <xsl:stylesheet version="1.0" 
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" /> 
 <!--V3-->

<xsl:template match="universite">
<xsl:apply-templates select="etudiant" >
<xsl:sort select="substring-after(nom,' ')" order="ascending"/>
</xsl:apply-templates>
</xsl:template>

<xsl:template match="etudiant">
  <p>Nom: <xsl:value-of select="nom" /></p>
  <p>Moyenne: <xsl:value-of select="format-number(sum(cours/@note) div count(cours), '##.0')"  /></p>
</xsl:template>


 </xsl:stylesheet>
