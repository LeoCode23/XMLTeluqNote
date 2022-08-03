<?xml version="1.0" ?> 
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="iso-8859-1" />
<xsl:template match="/">
    <xsl:apply-templates select="//client" />
</xsl:template>
<xsl:template match="client">
    <xsl:text>Nom du client: </xsl:text>
    <xsl:value-of select="@nom" />
<br/>

    <xsl:text>Somme: </xsl:text>
    <xsl:value-of select="sum(transaction/@montant)" />
 <br/>
</xsl:template>
</xsl:stylesheet>
