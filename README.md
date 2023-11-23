# Cyber
_________

 Cyber is a basic Phishing training service designed to help your company stay safe against phishing attacks. 
 Once running it can be modified and steered as you see fit, nothing is supposed to be hardcoded.

1. [Setup](#setup)
2. [Usage](#usage)
   1. [Templates](#templates)
   2. [Endpoints](#endpoints)
 
# Setup
In order to setup Cyber you will need to fill out application properties located in `/src/main/resources`. There is an example file to guide you through your setup.
Once properties are set you can jar the project using `mvnw package` and then run it as you please using `java -jar <your_jar_name>`. Example properties file can be seen below.

```properties
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/<your schema>
spring.datasource.username=<your DB username>
spring.datasource.password=<your DB password>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

cyber.mail.host=<your host>
cyber.mail.domain=<your domain>
cyber.mail.username=<your username>
cyber.mail.password=<your password>
cyber.mail.port=<your port>
cyber.templatesRoot=<path to your templates directory>
cyber.maxPoints=10
cyber.development=false
```

# Usage

# Templates
In order to use Cyber you will need to set two things up. First of all you will need a template html email setup in `<your templaets root>/emails/<your chosen category>/<template name>.html`.
If you wish to use images in your email please place them in `<your templaets root>/emails/<your chosen category>/resources` directory. They need to follow the naming convetion: `<number>_<name>`.
Example email can be seen below.

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>EASY EMAIL</title>
</head>
<body>
<p>TEST EASY</p>
<a href="%message.badUrl">TEST EASY LINK</a>
</body>
</html>
%message.subjects={["TEST TEMAT EASY 1", "TEST TEMAT EASY 2"]}
%message.senders={["test1@cyber.com", "test2@cyber.com", "test3@cyber.com"]}
%message.points={1}
```

All template parameters are explained in the table below.

|      Parameter       | Set by user |                                                                                          Description                                                                                          |
|:--------------------:|:-----------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|   %message.badUrl    |    False    |                                                                     URL that will lead to the server evaluating endpoint                                                                      |
|  %message.subjects   |    True     |                                                               List of subjects that can be chosen for the given email template                                                                |
|   %message.senders   |    True     |                                                                 List of senders that can be chosen for a given email template                                                                 |
|   %message.points    |    True     | Number of points that will be granted to the receiver when the email is reported.<br/>The number of points deducted upon clicking a link will be `maxPoints` (set in properties) - this value |
| %message.resource{i} |    False    |                                        URL that will lead to the image added in `resources` directory. The {i} paremeter is the number in image name.                                         |

# Endpoints

Cyber is operated via rest endpoints and json files containing phishing campaigns located in `<your templaets root>/tests`. The example JSON file can be found below.

```json
{
    "testName":"test1",
    "receivers":["test@cyber.com"],
    "receiverRoles":["IT"],
    "senders":["polsl@cyber.com"],
    "subjects":["Stypendium rektora"],
    "message":"medium_1.html",
    "messageCategory":"medium",
    "saveTest":true
}
```
|    Variable     |   Type   | Default Value |                                                                 Description                                                                  |
|:---------------:|:--------:|:-------------:|:--------------------------------------------------------------------------------------------------------------------------------------------:|
|    testName     |  String  |      ""       |                                                         Name of the test when saved.                                                         |
|    receivers    | String[] |     null      |                                  Emails of the receivers. If null emails will be sent to the random users.                                   |
|  receiverRoles  | String[] |     null      |            Roles of the random receivers to send the email to. If null emails will be sent to the random users with random roles.            |
|     senders     | String[] |     null      |                         Possible senders of the emails. If null the list of senders will be taken from the template.                         |
|    subjects     | String[] |     null      |                        Possible subjects of the emails. If null the list of subjects will be taken from the template.                        |
|     message     |  String  |      ""       |   Name of the template in the category. Requires `messageCategory` to also be set. If empty random template from category will be chosen.    |
| messageCategory |  String  |      ""       |                   Name of the category from which templates will be chosen from. If empty random category will be chosen.                    |
|    saveTest     | boolean  |     false     | Specifies whether the test will be saved in `<your templaets root>/tests`. If JSON with name `testName` already exists it will be overriden. |

The usage of endpoints used for triggering tests is listed in the table below.

| Endpoint |  Parameter   | Type |                                                       Description                                                       |
|:--------:|:------------:|:----:|:-----------------------------------------------------------------------------------------------------------------------:|
|  /email  | phishingTest | Body | Run test described in sent JSON file containing the phising campaign details. It takes priority over any other setting. |
|  /email  |   testName   | Url  |             Run previously saved JSON with a name `testName`. It will be overriden by JSON located in body.             |
|  /email  |      -       |  -   |                                     Run a JSON test with all values set to default.                                     |