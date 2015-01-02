Rally Integration for [Rock Star App](https://github.com/IDeaSCo/rockstar)
======================

Very naive standalone java program that publishes stars and badges to Rock Star Application.

###Requirements
- MySQL 5.5
- Java 7
- Ant
- Rally Account
 - Email Addresses configured for concerned users.
 

###Configuration
- rally_star.properties
 - mysql.jdbc.url=[MySQL JDBC URL]
 - mysql.user=[MySQL USER]
 - mysql.password=[Base 64 Encoded MySQL PASSWORD]
 - rockstar.postUrl=[Rock Star App URL e.g.  http://rockstar:8090/rockstar]
 - rockstar.alternatePostUrl=[Alternate Rock Star App URL e.g http://localhost:9090/rockstar]
 - rockstar.email=[Email address that this tool should use to post stars to Rock Star App e.g. rally.user@company.com ]
 - rally.url=https://rally1.rallydev.com
 - rally.user.name=[user name using which this tool can access Rally]
 - rally.user.password=[Base 64 encoded password for the user configured above]
 - rally.project=[Project name]

###Build App
 ant
###Run App
 ant run

###Badges & Stars
Badges | Stars | Reason
-------|-------|-------
Process Champ|1|For having planned estimates on story
Process Champ|1|For updating tasks
Delivery Champ|10|For completing the story
Delivery Champ|25|For getting story Accepted in first 2 days of iteration.
Delivery Champ|20|For getting story Accepted in first 4 days of iteration.
Delivery Champ|15|For getting story Accepted in first 6 days of iteration.
Delivery Champ|10|For getting story Accepted within the iteration.
Process Violator|-2|Not having planned estimates on story
Spillover Champ|-20|For story spill over
Spillover Champ|-20|For story spill over
Process Violator|-5|For not tasking story
Process Violator|-5|For leaving the story/defect in prior iteration in state other than Accepted
Process Violator|-5|For leaving the story/defect in prior iteration in state other than Accepted
Process Violator|-5|For not updating either of tasks

###Download
