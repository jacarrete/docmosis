function createCase(office) {
    const date = new Date();
    cy.get('.hmcts-primary-navigation__item:nth-child(2) > .hmcts-primary-navigation__link').click();
    cy.wait(3000)
    // Choose Employment, Office and Type
    cy.get('#cc-jurisdiction').select('EMPLOYMENT');
    cy.get('#cc-case-type').select(office);
    cy.get('#cc-event').select('Create Case');
    cy.get('.button').click();
    cy.get('.ng-untouched').submit();
    cy.wait(3000)
    // Enter date and submission ref
    cy.get('#receiptDate-day').type(date.getDate().toString());
    cy.get('#receiptDate-month').type((date.getMonth()+1).toString());
    cy.get('#receiptDate-year').type(date.getFullYear().toString());
    cy.get('#feeGroupReference').type('123456123456');
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.wait(3000)
    // Enter claimant details
    cy.get('#claimant_TypeOfClaimant-Individual').click();
    cy.get('#claimantIndType_claimant_first_names').type('Test');
    cy.get('#claimantIndType_claimant_last_name').type('Claimant');
    cy.get('#claimantType_claimant_addressUK_claimant_addressUK_postcodeInput').type('B44 8AS');
    cy.get('#claimantType_claimant_addressUK_claimant_addressUK .heading-h2').click();
    cy.get('.button-30').click();
    cy.wait(3000)
    cy.get('#claimantType_claimant_addressUK_claimant_addressUK_addressList').select('1: Object');
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.wait(3000)
    // Enter respondant details
    cy.get('.panel > .button').click();
    cy.get('#respondentCollection_0_respondent_name').type('Test Resp');
    cy.get('#respondentCollection_0_respondent_ACAS_question_No').click();
    cy.wait(3000)
    cy.get(':nth-child(4) > .form-label').click();
    cy.get('#respondentCollection_0_respondent_address_respondent_address_postcodeInput').type('B44 8AS');
    cy.get('.button-30').click();
    cy.wait(3000)
    cy.get('#respondentCollection_0_respondent_address_respondent_address_addressList').select('2: Object');
    cy.get('.form').submit();
    cy.wait(3000)
    // Claimant work address
    cy.get('#claimantWorkAddressQuestion_Yes').click();
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.wait(2000)
    // Other Stuff
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.wait(1000)
    // Claimant Represented
    cy.get('#claimantRepresentedQuestion_No').click();
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.wait(1000)
    // Submit create case
    cy.get('.button:nth-child(2)').click();
    cy.get('.check-your-answers').submit();
}

function caseDetails() {
    cy.get('#next-step').select('Case Details');
    cy.get('.button:nth-child(2)').click();
    cy.get('.event-trigger').submit();
    cy.get('#clerkResponsible').select('1: Abu Mamaniyat');
    cy.get('#fileLocation').select('Casework Table');
    cy.get('#conciliationTrack').select('No Track');
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.get('.button:nth-child(2)').click();
    cy.get('.check-your-answers').submit();

}

describe('Login into AAT, create a case and accept it', () => {
    it('should create a case in Leeds', function () {
        cy.aatLogin()
        createCase('Leeds - Singles')
        cy.acceptCase()
    });
})

describe('Login into AAT, create a case and reject it', () => {
    it('should create a case', function () {
        cy.aatLogin()
        createCase('Leeds - Singles')
        cy.rejectCase()
    });
})

describe('Login into AAT, create a case and modify details', () => {
    it('should create a case', function () {
        cy.aatLogin()
        createCase('Leeds - Singles')
        caseDetails()
    });
})

describe('Login, create, accept, claimant rep', () => {
    it('should create a case in Leeds', function () {
        cy.aatLogin()
        createCase('Leeds - Singles')
        cy.acceptCase()
        cy.addClaimantRepresentative()
        cy.addRespondentRepresentative()
        cy.addJurisdictions();
    });
})