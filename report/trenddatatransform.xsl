<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/02/xpath-functions" xmlns:xdt="http://www.w3.org/2005/02/xpath-datatypes">
<xsl:output method="text" encoding="utf-8" media-type="text/plain"/>

<xsl:template match="/">
var trendReportData = {
	<xsl:apply-templates select="TrendReports"/>
}
</xsl:template>
<xsl:template match="TrendReports">
	"projectName": "<xsl:value-of select="ProjectInfo/ProjectName"/>",
	"badSmellType": ["Empty Catch Block", "Dummy Handler", "Unprotected Main Program", "Nested Try Statement", "Careless Cleanup", "Exception Thrown From Finally Block"],
	"reports": [
	<xsl:for-each select="Report">
		{ "date": "<xsl:value-of select="DateTime"/>", "badSmellCount": [ "<xsl:value-of select="EHSmellList/EmptyCatchBlock" />", "<xsl:value-of select="EHSmellList/DummyHandler" />", "<xsl:value-of select="EHSmellList/UnprotectedMainProgram" />", "<xsl:value-of select="EHSmellList/NestedTryStatement" />", "<xsl:value-of select="EHSmellList/CarelessCleanup" />", "<xsl:value-of select="EHSmellList/ExceptionThrownFromFinallyBlock" />"]}<xsl:if test="position()!=last()">,</xsl:if>
	</xsl:for-each>
	]
</xsl:template>

</xsl:stylesheet>