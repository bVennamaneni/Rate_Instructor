# Rate_Instructor

BY: Bhavana Vennamaneni

PROJECT: RateInstructor App

DESCRIPTION

This is an Android application that accesses information about instructors and lets people rate them. 
* Uses a list view to present some identifying information about each instructor. 
* When the user selects an instructor they get a view of more detailed information about that instructor.
* It allows to rate an instructor on a scale of 1 to 5 stars and provide comments about an instructor.
* Caches the data downloaded from server.
* Stores copy of Instructor list, ratings and comments in a database on the device for off-line access.

It accesses and manipulates data, which is available in JSON format using a REST-like interface via http using the following links where 'n' is id of instructor and 'k' is rating.
GET URL's
*http://bismarck.sdsu.edu/rateme/list
*http://bismarck.sdsu.edu/rateme/instructor/n
*http://bismarck.sdsu.edu/rateme/comments/n
POST URL's
*http://bismarck.sdsu.edu/rateme/rating/n/k
*http://bismarck.sdsu.edu/rateme/comment/n

It requires network access to get details of instructors and allows to rate and comment only when connected to internet. 
When data is accessed by the application, it caches the data locally to avoid downloading the same data repeatedly. Cached data is deleted when user exits the application.

Uses volley RequestQueue to make network requests to get data and post comments/ratings.

LIBRARIES

Volley - HTTP library that makes networking for Android apps easier and most importantly, faster.
