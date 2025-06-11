Microservices and Their API Endpoints for Swiftly Global
Below is a breakdown of all identified microservices along with their respective API endpoints, categorized by functionality.

1. User Service
Handles user authentication, registration, profile management, and security.

Endpoints:
Method	   Endpoint	                     Description
POST	   /users/register	             Register a new user
POST	   /users/login	                 Authenticate user and issue JWT
POST	   /users/logout	             Invalidate user session/token
GET	       /users/{userId}	             Retrieve user profile
PUT	       /users/{userId}	             Update user details
DELETE	   /users/{userId}	             Delete user account
PUT	       /users/change-password	     Update user password
GET	       /users/preferences/{userId}	 Retrieve user preferences from MongoDB
PUT	       /users/preferences/{userId}	 Update user preferences
POST       /users/refresh-token          For secure JWT token renewal, reduce login friction. 

Not Implemented --POST	   /users/reset-password	     Send reset password link
Not Implemented --POST	   /users/verify-email	         Verify user email for account activation

2. Transaction Service
Handles money transfers, transaction processing, and compliance.

Endpoints:
Method	     Endpoint	                                       Description
POST	     /transactions/initiate	                           Create a new transaction request
GET	         /transactions/{transactionId}	                   Retrieve transaction details
GET	         /transactions/user/{userId}	                   List all transactions for a user
PUT	         /transactions/cancel/{transactionId}	           Cancel a transaction before processing
GET	         /transactions/status/{transactionId}	           Check transaction status
POST	     /transactions/confirm/{transactionId}	           Confirm a pending transaction
GET	         /transactions/compliance-check/{transactionId}	   Verify compliance with AML & PCI-DSS
POST	     /transactions/webhook/callback	                   Handle external transaction notifications

3. Payment Gateway Service
Integrates with third-party payment providers for processing payments.

Endpoints:
Method	    Endpoint	                    Description
POST	    /payments/process	            Initiate payment via external provider
GET	        /payments/{paymentId}	        Retrieve payment details
GET	        /payments/status/{paymentId}	Check payment status
POST	    /payments/refund/{paymentId}	Request a refund
GET	        /payments/providers	            List available payment providers
POST	    /payments/webhook/callback	    Handle payment provider webhook events

4. Currency Exchange Service
Handles real-time currency conversion and exchange rates.

Endpoints:
Method	     Endpoint	                       Description
GET	         /exchange/rates	               Fetch current exchange rates from CurrencyLayer
GET	         /exchange/rates/{from}/{to}       Get exchange rate for specific currency pair
POST	     /exchange/convert	               Convert an amount between currencies
GET	         /exchange/history/{from}/{to}	   Retrieve historical exchange rates
GET	         /exchange/cached-rates	           Retrieve cached exchange rates from Redis

5. Notification Service
Manages real-time notifications and email alerts.

Endpoints:
Method	     Endpoint	                   Description
POST	     /notifications/send-email	   Send an email notification
POST	     /notifications/send-sms	   Send an SMS notification
POST	     /notifications/send-push	   Send a push notification
GET	         /notifications/user/{userId}  Get a user's notification history
GET          /notifications/ws/status      Monitor WebSocket connection health/status. 
WS	         /notifications/ws	           WebSocket connection for real-time alerts

6. Audit & Logging Service
Handles system logs, transaction logs, and regulatory audit trails.

Endpoints:
Method	     Endpoint	                   Description
GET	         /audit/logs	               Retrieve system logs
GET	         /audit/logs/{transactionId}   Retrieve logs related to a specific transaction
GET	         /audit/logs/user/{userId}	   Retrieve logs related to a user
POST	     /audit/logs	               Store a new log entry
DELETE	     /audit/logs/{logId}	       Delete a log entry (admin only)

7. Account Confirmation & Email Notification Service
Handles account verification emails and notification emails.

Endpoints:
Method	     Endpoint	                       Description
POST	     /account/confirm-email	           Send an account confirmation email
POST	     /account/resend-confirmation	   Resend confirmation email
POST	     /account/reset-password	       Send a password reset email
POST	     /account/verification-status	   Check if user email is verified

8. API Gateway Service (Spring Cloud Gateway)
Acts as an entry point for routing requests to microservices.

Endpoints:
Method	     Endpoint	       Description
GET	         /health	       Check API gateway health
GET	         /status	       Get system-wide status overview
GET	         /rate-limits	   Check
GET          /metrics          Allow Prometheus to scrape API Gateway health metrics directly. 
