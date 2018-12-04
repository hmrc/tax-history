# Tax History

[ ![Download](https://api.bintray.com/packages/hmrc/releases/tax-history/images/download.svg) ](https://bintray.com/hmrc/releases/tax-history/_latestVersion)


Source code for the back end microservice for the Income Record Viewer service

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

### Running the application

In order to run the microservice, you must have SBT installed. You should then be able to start the application using:

> ```sbt run {PORTNUM}```
> The port used for this project is 9997

> To run the tests for the application, you can run: ```sbt test```
> or to view coverage run: ```sbt coverage test coverageReport```

## RESTful Service Endpoints
We have adhered to a hypermedia [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) design allowing the api to describe its own use at runtime by providing URI's to further api calls.

#### Get List of Employments
Gets a list of employment objects for a given nino and tax year

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxyear/employments``` | GET | Retrieves all employments for an given nino and tax year as below. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |

**Return Format**
```
[
  Employment {
    employmentId: String UUID Format
    startDate:LocalDate,
    endDate:Option[LocalDate],
    payeReference:String,
    employerName:String,
    companyBenefitsURI: Option[String] (e.g. /:taxyear/employments/:employmentId/company-benefits),
    payAndTaxURI: Option[String] (e.g. /:taxyear/employments/:employmentId/pay-and-tax),
    employmentURI: Option[String] (e.g. /:taxyear/employments/:employmentId),
    employmentPaymentType: Option[String] (see note below),
    employmentStatus: Int (Live=1, PotentiallyCeased=2, Ceased=3, Unknown=99),
    worksNumber:String
  }
]
```

The `employmentPaymentType` field, if present, will have one of the following values:
- "OccupationalPension"
- "JobseekersAllowance"
- "IncapacityBenefit"
- "EmploymentAndSupportAllowance"
- "StatePensionLumpSum"

#### Get individual Employment
Gets an employment object for a given nino and tax year and employmentId

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxyear/employments/:employmentId``` | GET | Retrieves individual employment for an given nino and tax year and employmentId as below. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |
| ```:employmentId```| true | Unique UUID in the standard format e.g. 123e4567-e89b-12d3-a456-426655440000 |

**Return Format**
```
Employment{
  employmentId: String UUID Format
  startDate:LocalDate,
  endDate:Option[LocalDate],
  payeReference:String,
  employerName:String,
  companyBenefitsURI: Option[String] (e.g. /:taxyear/employments/:employmentId/company-benefits),
  payAndTaxURI: Option[String] (e.g. /:taxyear/employments/:employmentId/pay-and-tax),
  employmentURI: Option[String] (e.g. /:taxyear/employments/:employmentId),
  employmentPaymentType: Option[String] (see note below),
  employmentStatus: Int (Live=1, PotentiallyCeased=2, Ceased=3, Unknown=99),
  worksNumber:String
}
```

The `employmentPaymentType` field, if present, will have one of the following values:
- "OccupationalPension"
- "JobseekersAllowance"
- "IncapacityBenefit"
- "EmploymentAndSupportAllowance"
- "StatePensionLumpSum"

#### Get List of Allowances
Gets a list of allowance objects for a given nino and tax year

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxyear/allowances``` | GET | Retrieves all allowances for an given nino and tax year as below. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |

**Return Format**
```
[
    Allowance{
      allowanceId: String UUID Format,
      iabdType: String,
      amount: BigDecimal
    }
]
```

#### Get List of Company Benefits
Gets a list of company benefits objects for a given nino, tax year and employmentId

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxyear/employments/:employmentId/company-benefits``` | GET | Retrieves all company benefits for an given nino, tax year and employmentId as below. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |
| ```:employmentId```| true | Unique UUID in the standard format e.g. 123e4567-e89b-12d3-a456-426655440000 |

**Return Format**
```
[
    CompanyBenefit{
      companyBenefitId: String UUID Format,
      iabdType: String,
      amount: BigDecimal,
      source: Option[Int],
      isForecastBenefit: Boolean
    }
]
```

#### Get Pay and Tax
Gets pay and tax object containing a list of EYU's for a given nino, tax year and employmentId

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxyear/employments/:employmentId/pay-and-tax``` | GET | Retrieves all pay and tax and EYU for an given nino, tax year and employmentId as below. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |
| ```:employmentId```| true | Unique UUID in the standard format e.g. 123e4567-e89b-12d3-a456-426655440000 |

**Return Format**
```
[
    PayAndTax{
        payAndTaxId: String UUID Format,
        taxablePayTotal: Option[BigDecimal],
        taxablePayTotalIncluldingEYU: Option[BigDecimal],
        taxTotal: Option[BigDecimal],
        taxTotalIncludingEYU: Option[BigDecimal],
        studentLoan: Option[BigDecimal],
        studentLoanIncludingEYU: Option[BigDecimal],
        paymentDate: Option[LocalDate],
        earlierYearUpdates: List[
            EarlierYearUpdate{
                earlierYearUpdateId: String UUID Format,
                taxablePayEYU: BigDecimal,
                taxEYU: BigDecimal,
                studentLoanEYU: Option[BigDecimal],
                receivedDate: LocalDate
            }
        ]
    }
]
```

#### Get Tax Years
Gets list of tax years that can be used to query for employments and allowances for a given nino

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/tax-years``` | GET | Retrieves all tax years for a given nino. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |

**Return Format**
```
[
    IndividualTaxYear {
        year : Int,
        allowancesURI : String (e.g. /2016/allowances),
        employmentsURI : String (e.g. /2016/employments),
        taxAccountURI : String (e.g. /2016/tax-account)
    }
]
```

#### Get Tax Account
Gets tax account information for a given nino and tax year

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:year/tax-account``` | GET | Retrieves all tax account information for a given nino and tax year. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |

**Responses**

404 - if there is no tax account information for the nino and tax year

200 - if there is tax account information, the response will contain a json object like:

```
    TaxAccount {
        taxAccountId :  String UUID Format,,
        outstandingDebtRestriction: Option[BigDecimal],
        underpaymentAmount: Option[BigDecimal],
        actualPUPCodedInCYPlusOneTaxYear: Option[BigDecimal]
    }
```

#### Get Income Source
Gets income source details for a given nino, tax year and employment

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxYear/employments/:employmentId/income-source``` | GET | Retrieves the income source for a given nino, tax year and employmentId. |

**Parameters**
|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |
| ```:employmentId```| true | Unique UUID in the standard format e.g. 123e4567-e89b-12d3-a456-426655440000 |

**Responses**

404 - if there is no income source details for the employment

200 - if there is income source details, the response will contain a json object like:
```
    IncomeSource{
        "employmentId": Int,
        "employmentType": Int,
        "actualPUPCodedInCYPlusOneTaxYear": Option[BigDecimal],
        "deductions": [
            TaDeduction{
                "type": Int,
                "npsDescription": String,
                "amount": BigDecimal,
                "sourceAmount": Option[BigDecimal]
            }
        ],
        "allowances": [
            TaAllowance{
                "type": Int,
                "npsDescription": String,
                "amount": BigDecimal,
                "sourceAmount": Option[BigDecimal],
            }
        ],
        "basisOperation": Option[Int],
        "employmentTaxDistrictNumber": Int,
        "employmentPayeRef": String,
        "taxCode": String
    }
```

#### Get All Details
Gets all information for a given nino and tax year. This includes:
- all employments
- a list of company benefits, if available, for each employment
- a list of pay and tax details, if available, for each employment. Each pay and tax detail will also have a list of their Earlier Year Updates (EYUs).
- a list of income sources, if available, for each employment
- all allowances
- the tax account information if available
- state pension details if available

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:year/all-details``` | GET | Retrieves all details for a given nino and tax year. |

**Parameters**

|*Parameter*|*Required*|*Description*|
|----|----|----|
| ```:nino```| true | Standard National Insurance Number format e.g. QQ123456A |
| ```:taxyear```| true | The first year in a tax year. e.g. for tax year 6th April 2016 - 5th April 2017 would be ```2016``` |

**Authorisation**

The logged in user must be either:
- An individual who has a NINO that matches the requested NINO in the URL
- An agent with an IR-SA-AGENT enrolment and who has a delegated IR-SA enrolment for the client identified by the requested NINO (where the IR-SA enrolment's UTR matches the client's SA UTR). The `citizen-details` service is used to lookup the client's SA UTR for their NINO - if no SA UTR is available from `citizen-details` access is not allowed.

**Responses**

401 - If there is no active session

403 - If there is an active session, but they do not have authorisation to access the requested NINO's details. See note on Authorisation above.

404 - If no data was found

200 - With a response format as below:
```
{
  "employments": [
    Employment{
      "employmentId" : String UUID
      "startDate" : LocalDate,
      "endDate" : Option[LocalDate],
      "payeReference" : String,
      "employerName" : String,
      "employmentPaymentType" : Option[String] (see note below),
      "employmentStatus" : Int (Live=1, PotentiallyCeased=2, Ceased=3, Unknown=99),
      "worksNumber" : String,
      "companyBenefitsURI" : Option[String],
      "payAndTaxURI" : Option[String],
      "employmentURI" : Option[String]
    }
  ],

  "allowances" : [
    Allowance{
      "allowanceId": String UUID,
      "iabdType": String,
      "amount": BigDecimal
    }
  ],

  "benefits" : {
    "[employmentId UUID]" : [
      CompanyBenefit{
        "companyBenefitId" : String UUID,
        "iabdType" : String,
        "amount" : BigDecimal,
        "source" : Option[Int],
        "isForecastBenefit" : Boolean
      }
    ]
  },

  "payAndTax" : {
    "[employmentId UUID]" : PayAndTax{
      "payAndTaxId" : String UUID,
      "taxablePayTotal" : Option[BigDecimal],
      "taxablePayTotalIncluldingEYU": Option[BigDecimal],
      "taxTotal" : Option[BigDecimal],
      "taxTotalIncludingEYU": Option[BigDecimal],
      "studentLoan" : Option[BigDecimal],
      "studentLoanIncludingEYU" : Option[BigDecimal],
      "paymentDate" : Option[LocalDate],
      "earlierYearUpdates" : [
        EarlierYearUpdate{
          "earlierYearUpdateId" : String UUID,
          "taxablePayEYU" : BigDecimal,
          "taxEYU" : BigDecimal,
          "studentLoanEYU" : Option[BigDecimal],
          "receivedDate" : LocalDate
        }
      ]
    }
  },

  "incomeSources" : {
    "[employmentId UUID]" : IncomeSource{
      "employmentId" : Int,
      "employmentType" : Int,
      "actualPUPCodedInCYPlusOneTaxYear" : Option[BigDecimal],
      "deductions" : [
        TaDeduction{
          "type": Int,
          "npsDescription": String,
          "amount": BigDecimal,
          "sourceAmount": Option[BigDecimal]
        }
      ],
      "allowances" : [
        TaAllowance{
          "type": Int,
          "npsDescription": String,
          "amount": BigDecimal,
          "sourceAmount": Option[BigDecimal]
        }
      ],
      "taxCode" : String,
      "basisOperation" : Option[Int],
      "employmentTaxDistrictNumber" : Int,
      "employmentPayeRef" : String
    }
  },

  "taxAccount" : Option[TaxAccount]{
    "taxAccountId" : String UUID,
    "outstandingDebtRestriction" : Option[BigDecimal],
    "underpaymentAmount" : Option[BigDecimal],
    "actualPUPCodedInCYPlusOneTaxYear" : Option[BigDecimal]
  },

  "statePension" : Option[StatePension]{
    "grossAmount" : BigDecimal,
    "typeDescription" : String
  }
}
```

The `benefits`, `payAndTax`, and `incomeSources` objects will map an employment's `employmentId` UUID to an object containing details for that employment. If an employment has no details, then there will be no UUID key present for that employment.

Empty list, empty map and optional handling:
- For the lists (`employments`, `allowances`), if there are no list elements, the key will still be present in the json but with an empty list value, e.g. `"allowances" : []`
- For the maps (`benefits`, `payAndTax`, `incomeSources`), if there are no employments with any details, the map's key will still be present but the map's object value would be an empty object, e.g. `"benefits" : {}`.
- For the optionals (`taxAccount`, `statePension`), if they are not available then the key/value entry will be missing from the json.

The `employmentPaymentType` field, if present, will have one of the following values:
- "OccupationalPension"
- "JobseekersAllowance"
- "IncapacityBenefit"
- "EmploymentAndSupportAllowance"
- "StatePensionLumpSum"