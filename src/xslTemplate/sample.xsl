<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/02/xpath-functions" xmlns:xdt="http://www.w3.org/2005/02/xpath-datatypes">
<xsl:template match="/EHSmellReport">
<html>
<head>
<title>EH Smells Results</title>
<LINK href="styles.css" type="text/css" rel="stylesheet"></LINK>
</head>
<body >
	<table cellSpacing="0" cellPadding="0" width="100%" border="0" align="center"><tr><td>
	<span class="supertitle">EH Smells Report </span><a name="top"></a><div align="left" class="description"></div><h2 class="bigtitle">Summary<a name="Summary"></a></h2>
	<p>[<a class="menulink" href="#Summary">Summary</a>] [<a class="menulink" href="#EH_Smells_List">EH Smells List</a>] [<a class="menulink" href="#Code_Info_List">Code Information List</a>] [<a class="menulink" href="#Package_List">Package List</a>] </p>
		<div align="left" class="menutext"> <b><font size="3">Project Name: <xsl:value-of select="Summary/ProjectName"></xsl:value-of></font></b></div>
		<div align="left" class="menutext"> <b><font size="3">Generation Time : <xsl:value-of select="Summary/DateTime"></xsl:value-of></font></b></div>
		<div align="left" class="menutext"> <b><font size="3">Filter conditions : <xsl:value-of select="Summary/Filter"></xsl:value-of></font></b></div>
	<h2 class="smalltitle">EH Smells Summary</h2>
	<div align="center" id="reportSrcDiv">
	<img>
	<xsl:attribute name="src">
	<xsl:value-of select="Summary/JPGPath"></xsl:value-of>
	</xsl:attribute>
	</img>
	</div>
	<h2 class="bigtitle">EH Smells List<a name="EH_Smells_List"></a></h2>
	<p>[<a class="menulink" href="#Summary">Summary</a>] [<a class="menulink" href="#EH_Smells_List">EH Smells List</a>] [<a class="menulink" href="#Code_Info_List">Code Information List</a>] [<a class="menulink" href="#Package_List">Package List</a>] </p>
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
	<div align="right"><a href="#top">Top</a></div>
	<p></p>
	<p></p>
		<h2 class="bigtitle">Code Information List<a name="Code_Info_List"></a></h2>
	<p>[<a class="menulink" href="#Summary">Summary</a>] [<a class="menulink" href="#EH_Smells_List">EH Smells List</a>] [<a class="menulink" href="#Code_Info_List">Code Information List</a>] [<a class="menulink" href="#Package_List">Package List</a>] </p>
	<table width="100%" border="0" cellpadding="2" cellspacing="1" class="forumline" id="table3"><thead>
  <tr bgcolor="#EAEADA" class="bigbluetext"><th>Lines Of Code (LOC)</th><th>
	Number Of Try Block</th><th>Number Of Catch Clause</th><th>
	Number Of Finally Block</th></tr></thead>
	<tr bgcolor="#F7F7F7" class="text" align="right">
		<td> <xsl:value-of select="CodeInfoList/LOC"></xsl:value-of> </td>
		<td><xsl:value-of select="CodeInfoList/TryNumber"></xsl:value-of></td>
		<td><xsl:value-of select="CodeInfoList/CatchNumber"></xsl:value-of></td>
		<td><xsl:value-of select="CodeInfoList/FinallyNumber"></xsl:value-of></td>
	</tr></table>
	<div align="right"><a href="#top">Top</a></div>
	<p></p>
	<hr></hr>
	<p></p>
	<h2 class="bigtitle">Package List<a name="Package_List"></a></h2>
	<p>[<a class="menulink" href="#Summary">Summary</a>] [<a class="menulink" href="#EH_Smells_List">EH Smells List</a>] [<a class="menulink" href="#Code_Info_List">Code Information List</a>] [<a class="menulink" href="#Package_List">Package List</a>] </p>
	
	<table width="100%" border="0" cellpadding="2" cellspacing="1" class="forumline" id="table3"><thead>
	<h></h>
  	<tr bgcolor="#EAEADA" class="bigbluetext"><th class="forumline1" width="133">Package Name</th><th>Ignore Checked Exception</th><th>
		Dummy Handler</th><th>Unprotected Main Program</th><th>
		Nested Try Block</th><th>Total EH Smells</th></tr></thead>
	<xsl:for-each select="AllPackageList/Package">
		<tr bgcolor="#F7F7F7" class="text" align="right">
			<td align="left" class="forumline1" width="133" height="23">
				<a>
					<xsl:attribute name="href">
					<xsl:value-of select="HrefPackageName"></xsl:value-of>
					</xsl:attribute>
					<xsl:value-of select="PackageName"></xsl:value-of>
				</a>
			</td>
			<td><xsl:value-of select="IgnoreCheckedException"></xsl:value-of></td>
			<td><xsl:value-of select="DummyHandler"></xsl:value-of></td>
			<td><xsl:value-of select="UnprotectedMainProgram"></xsl:value-of></td>
			<td><xsl:value-of select="NestedTryBlock"></xsl:value-of></td>
			<td><xsl:value-of select="PackageTotal"></xsl:value-of></td>
		</tr>
	</xsl:for-each>
	<tr bgcolor="#CCCCCC" class="text" align="right">
		<td align="left" bgcolor="#E5E5E5" class="forumline1" width="133"><span class="text2"><strong>Total</strong></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="AllPackageList/Total/IgnoreTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="AllPackageList/Total/DummyTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="AllPackageList/Total/UnMainTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="169"><span class="text2"><xsl:value-of select="AllPackageList/Total/NestedTrTotal"></xsl:value-of></span></td>
		<td bgcolor="#E5E5E5" width="170"><span class="text2"><xsl:value-of select="AllPackageList/Total/AllTotal"></xsl:value-of></span></td>
	</tr>
	</table>
	<div align="right"><a href="#top">Top</a></div>

	<p></p>
	<hr></hr>
	<h2 class="smalltitle">Package List</h2>
	
	<xsl:for-each select="PackageList/Package">
		<h2 class="smalltitle"><span class="title">Package </span><xsl:value-of select="PackageName"></xsl:value-of>
			<a>
				<xsl:attribute name="name">
				<xsl:value-of select="PackageName"></xsl:value-of>
				</xsl:attribute>
			</a>
		</h2>
		<p></p>
		<table width="100%" border="0" cellpadding="2" cellspacing="1" class="forumline" id="table2">
			<thead>
				<tr style="text-align: center" bgcolor="#EAEADA" class="bigbluetext">
					<th style="text-align: center" class="forumline1">Class Name</th>
					<th style="text-align: center" class="forumline1" width="229">Method</th>
					<th style="text-align: center" class="forumline1" width="237">Smell Type</th>
					<th style="text-align: center" class="forumline1" width="267"> Line</th>
				</tr>
			</thead>
			<xsl:for-each select="ClassList/SmellData">
				<tr bgcolor="#F7F7F7" class="text" align="right">
					<td class="forumline1">
						<a class="menulink">

							<xsl:choose>
							<xsl:when test="State='0'">
								<xsl:attribute name="href">
								<xsl:value-of select="LinkCode"></xsl:value-of>
								</xsl:attribute>
							</xsl:when>
							</xsl:choose>

							<xsl:value-of select="ClassName"></xsl:value-of>
						</a>
					</td>
					<td><xsl:value-of select="MethodName"></xsl:value-of></td>
					<td><xsl:value-of select="SmellType"></xsl:value-of></td>
					<td><xsl:value-of select="Line"></xsl:value-of></td>
				</tr>
			</xsl:for-each>
			<tr bgcolor="#E5E5E5" class="text" align="right">
				<td style="text-align: left" class="forumline1" colspan="2"><span class="text2"><strong>Total EH Smell</strong></span></td>
				<td align="left" colspan="2"><span class="text2"><xsl:value-of select="Total"></xsl:value-of></span></td>
			</tr>
		</table>

		<div align="right"><a href="#top">Top</a></div>
		<hr></hr>
	</xsl:for-each>

	<div align="center"><span class="orangetext">@ copy; 2009, <a href="http://pl.csie.ntut.edu.tw/ntutlab306/index.php" >Software Systems Lab</a> at <a href="http://www.ntut.edu.tw/" >NTUT</a></span></div></td></tr></table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>