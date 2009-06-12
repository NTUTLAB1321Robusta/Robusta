<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/02/xpath-functions" xmlns:xdt="http://www.w3.org/2005/02/xpath-datatypes">
<xsl:template match="/EHSmellReport">
<html>
<head>
<title>EH Smell Results</title>
<LINK href="styles.css" type="text/css" rel="stylesheet"></LINK>
</head>
<body><table cellSpacing="0" cellPadding="0" width="100%" border="0" align="center"><tr><td>
	<span class="supertitle">EH Smell Report </span><a name="top"></a><div align="left" class="description"></div><h2 class="bigtitle">Summary<a name="Summary"></a></h2>
		<div align="left" class="menutext"> <b><font size="3">Project : <xsl:value-of select="Summary/ProjectName"></xsl:value-of></font></b></div>
		<div align="left" class="menutext"> <b><font size="3">Time : <xsl:value-of select="Summary/DateTime"></xsl:value-of></font></b></div>	
	<h2 class="smalltitle">EH Smell Summary</h2>
	<div align="center">
	<img src ="Report.jpg"></img>
	</div>
	<h2 class="bigtitle">EH Smell List<a name="Debug Sessions"></a></h2>
	<table width="100%" border="0" cellpadding="2" cellspacing="1" class="forumline" id="table3"><thead>
  <tr bgcolor="#EAEADA" class="bigbluetext"><th>Ignore Checked Exception</th><th>
	Dummy Handler</th><th>Unprotected Main Program</th><th>
	Nested Try Block</th><th>Total EH Smells</th></tr></thead>
	<tr bgcolor="#F7F7F7" class="text" align="right">
		<td> <xsl:value-of select="EHSmellList/IgnoreCheckedException"></xsl:value-of> </td>
		<td><xsl:value-of select="EHSmellList/DummyHandler"></xsl:value-of></td>
		<td><xsl:value-of select="EHSmellList/UnprotectedMainProgram"></xsl:value-of></td>
		<td><xsl:value-of select="EHSmellList/NestedTryBlock"></xsl:value-of></td>
		<td><xsl:value-of select="EHSmellList/Total"></xsl:value-of></td>
	</tr></table>
	<p></p>
	<p></p>
	<hr></hr>
	<p></p>
	<h2 class="bigtitle">Package List<a name="Debug Sessions"></a></h2>
	<table width="100%" border="0" cellpadding="2" cellspacing="1" class="forumline" id="table3"><thead>
	<h></h>
  	<tr bgcolor="#EAEADA" class="bigbluetext"><th class="forumline1" width="133">Package Name</th><th>Ignore Checked Exception</th><th>
		Dummy Handler</th><th>Unprotected Main Program</th><th>
		Nested Try Block</th><th>Total EH Smells</th></tr></thead>
	<xsl:for-each select="PackageList">
		<tr bgcolor="#F7F7F7" class="text" align="right">
			<td align="left" class="forumline1" width="133" height="23"><a href="#chewei.cc"><xsl:value-of select="PackageName"></xsl:value-of></a></td>
			<td><xsl:value-of select="IgnoreCheckedException"></xsl:value-of></td>
			<td><xsl:value-of select="DummyHandler"></xsl:value-of></td>
			<td><xsl:value-of select="UnprotectedMainProgram"></xsl:value-of></td>
			<td><xsl:value-of select="NestedTryBlock"></xsl:value-of></td>
			<td><xsl:value-of select="Total"></xsl:value-of></td>
		</tr>
	</xsl:for-each>
	<tr bgcolor="#CCCCCC" class="text" align="right">
		<td align="left" bgcolor="#E5E5E5" class="forumline1" width="133"><span class="text2"><strong>Total</strong></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="PackageListTotal/IgnoreTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="PackageListTotal/DummyTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="PackageListTotal/UnMainTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="PackageListTotal/NestedTrTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="170"><span class="text2"><xsl:value-of select="PackageListTotal/PackagesTotal"></xsl:value-of></span></td>
	</tr>
	</table>

	<p></p>
	<div align="center"><span class="orangetext">@ copy; 2009, <a href="http://pl.csie.ntut.edu.tw/ntutlab306/index.php" >Software Systems Lab</a> at <a href="http://www.ntut.edu.tw/" >NTUT</a></span></div></td></tr></table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>