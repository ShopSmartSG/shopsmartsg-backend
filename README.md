# shopsmartsg-backend  - initial commit
1. Added Jacoco plugin to generate code coverage report  : 90% coverage
2. Added basic HomeController
3. excluded DataSourceAutoConfiguration as we don't have MongoDB functionality added yet
4. Added basic spring dependecies like web, devtools, test, lombok

# Chain of Responsibility Design Pattern
Internally uses Chain of Responsibility Software Design pattern to link different parts (chain-links) of login flows.
#

# Integration of Google Cloud Identity Platform for OIDC auth
Here we have integrated Google Cloud Identity Platform (GCIP) for federated identity management and user authentication.
Using this we track Google backed and 2FA (OTP + Password) login flow for user.
#
