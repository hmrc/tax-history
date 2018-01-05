# Tax-History ( Agents for Individuals )

[ ![Download](https://api.bintray.com/packages/hmrc/releases/tax-history/images/download.svg) ](https://bintray.com/hmrc/releases/tax-history/_latestVersion)


Source code for the back end microservice for the Agents for Individuals service

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
    endtDate:Option[LocalDate],
    payeReference:String,
    employerName:String,
    companyBenefitsURI: Option[String] (e.g. /:taxyear/employments/:employmentId/company-benefits),
    payAndTaxURI: Option[String] (e.g. /:taxyear/employments/:employmentId/pay-and-tax),
    employmentURI: Option[String] (e.g. /:taxyear/employments/:employmentId)
  }
]
```

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
  endtDate:Option[LocalDate],
  payeReference:String,
  employerName:String,
  companyBenefitsURI: Option[String] (e.g. /:taxyear/employments/:employmentId/company-benefits),
  payAndTaxURI: Option[String] (e.g. /:taxyear/employments/:employmentId/pay-and-tax),
  employmentURI: Option[String] (e.g. /:taxyear/employments/:employmentId)
}
```

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
      amount: BigDecimal
    }
]
```

#### Get Pay and Tax
Gets pay and tax object containing a list of EYU's for a given nino, tax year and employmentId

| *URL* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/:nino/:taxyear/employments/:employmentId/pay-and-tax``` | GET | Retrieves all company benefits for an given nino, tax year and employmentId as below. |

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
         taxTotal: Option[BigDecimal],
         paymentDate: Option[LocalDate],
         earlierYearUpdates: List[EarlierYearUpdate]

         ## - Above eyu list is of the below objects
        EarlierYearUpdate{
            earlierYearUpdateId: String UUID Format,
            taxablePayEYU: BigDecimal,
            taxEYU: BigDecimal,
            receivedDate:LocalDate
        }
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

**Return Format**
```
    TaxAccount {
        taxAccountId :  String UUID Format,,
        outstandingDebtRestriction: Option[BigDecimal],
        underpaymentAmount: Option[BigDecimal],
        actualPUPCodedInCYPlusOneTaxYear: Option[BigDecimal]
    }
```

