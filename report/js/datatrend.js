var trendReportData = {
	"badSmellType": ["Empty Catch Block", "Dummy Handler", "Unprotected Main Program", "Nested Try Statement", "Careless Cleanup", "Over Logging", "Thrown Exception In Finally Block"],
	"reports": [
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]},
		{"date": "May 15, 2014 5:23:20 PM CST", "badSmellCount": [Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200), Math.floor(Math.random()*200)]}
	]
};

var allPastReport = [];
$.each(trendReportData.reports, function(reportIndex, report){
	// reportIndex == report.id (i.e. x value), y value is badSmellCount of that serieIndex
	allPastReport.push([reportIndex,report.date]);
	for(var i=0;i<report.badSmellCount.length;i++)
		allPastReport[reportIndex].push(report.badSmellCount[i]);
	var total = eval( report.badSmellCount.join('+') );
	allPastReport[reportIndex].push(total);
});