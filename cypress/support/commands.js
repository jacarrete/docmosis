// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
const date = new Date();

Cypress.Commands.add('aatLogin', () => {
    cy.visit('https://manage-case.aat.platform.hmcts.net')
    cy.get('#username').type('eric.ccdcooper@gmail.com').should('have.value', 'eric.ccdcooper@gmail.com');
    cy.get('#password').type('Nagoya0102').should('have.value', 'Nagoya0102');
    cy.wait(1000)
    cy.get('.button').click()
    cy.wait(5000);
})

Cypress.Commands.add('rejectCase' , () => {
    cy.get('#next-step').select('Accept/Reject Case');
    cy.get('.button:nth-child(2)').click();
    cy.get('#preAcceptCase_caseAccepted_No').click();
    cy.wait(1000)
    cy.get('#dateRejected-day').type(date.getDate().toString());
    cy.get('#dateRejected-month').type((date.getMonth()+1).toString());
    cy.get('#dateRejected-year').type(date.getFullYear().toString());
    cy.get('fieldset > :nth-child(2) > .form-label').click();
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.get('.button:nth-child(2)').click();
    cy.get('.check-your-answers').submit();
})

Cypress.Commands.add('acceptCase', () => {
    cy.get('#next-step').select('Accept/Reject Case');
    cy.get('.button:nth-child(2)').click();
    cy.get('#preAcceptCase_caseAccepted_Yes').click();
    cy.get('#dateAccepted-day').type(date.getDate().toString());
    cy.get('#dateAccepted-month').type((date.getMonth()+1).toString());
    cy.get('#dateAccepted-year').type(date.getFullYear().toString());
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    // Submit page
    cy.get('.button:nth-child(2)').click();
    cy.get('.check-your-answers').submit();
    cy.wait(1000)
})

Cypress.Commands.add('addClaimantRepresentative', () => {
    cy.get('#next-step').select('Claimant Representative');
    cy.get('.button:nth-child(2)').click();
    cy.get('#claimantRepresentedQuestion_Yes').click();
    cy.get('#representativeClaimantType_name_of_representative').type('Claimant Rep');
    cy.get('#representativeClaimantType_name_of_organisation').type('Claimant Org');
    cy.get('#representativeClaimantType_representative_occupation').select('Solicitor');
    cy.get('#representativeClaimantType_representative_address_representative_address_postcodeInput').type('B44 8AS');
    cy.get('.button-30').click();
    cy.wait(1000)
    cy.get('#representativeClaimantType_representative_address_representative_address_addressList').select('16: Object');
    cy.get('.button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.get('.button:nth-child(2)').click();
    cy.get('.check-your-answers').submit();
})

Cypress.Commands.add('addRespondentRepresentative', () => {
    cy.get('#next-step').select('Respondent Representative');
    cy.get('.button:nth-child(2)').click();
    cy.get('.panel > .button').click();
    cy.get('#repCollection_0_resp_rep_name').type('Test Resp').should('have.value', 'Test Resp');
    cy.get('#repCollection_0_name_of_representative').type('Respondent Representative One').should('have.value', 'Respondent Representative One');
    cy.get('#repCollection_0_representative_occupation').select('Union');
    cy.get('#repCollection_0_representative_address_representative_address_postcodeInput').type('B44 8AS').should('have.value', 'B44 8AS')
    cy.get('.button-30').click();
    cy.wait(1000)
    cy.get('#repCollection_0_representative_address_representative_address_addressList').select('5: Object');
    cy.get('.button:nth-child(3)').click();
    cy.get('.form').submit();
    cy.get('.check-your-answers').submit();
})

Cypress.Commands.add('addJurisdictions', () => {
    cy.get('#next-step').select('Jurisdiction');
    cy.get('.button:nth-child(2)').click();
    cy.get('.panel > .button').click()
    cy.get('#jurCodesCollection_0_juridictionCodesList').select('ADG');
    cy.get('#jurCodesCollection_0_judgmentOutcome').select('Not allocated');
    cy.get('.button:nth-child(4)').click();
    cy.get('#jurCodesCollection_1_juridictionCodesList').select('ADT');
    cy.get('#jurCodesCollection_1_judgmentOutcome').select('Not allocated');
    cy.get('.form-group > .button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.get('.check-your-answers').submit();
})

Cypress.Commands.add('listHearing', () => {
    cy.get('#next-step').select('List Hearing');
    cy.get('.button:nth-child(2)').click();
    cy.get('.panel > .button').click();
    cy.get('#hearingCollection_0_hearingNumber').type('1');
    cy.get('#hearingCollection_0_Hearing_type').select('Preliminary Hearing');
    cy.get('#hearingCollection_0_hearingPublicPrivate').select('Public');
    cy.get('#hearingCollection_0_hearingFormat-Hybrid').click();        
    cy.get('#hearingCollection_0_Hearing_venue').select('Leeds');
    cy.get('#hearingCollection_0_hearingEstLengthNum').type('1');
    cy.get('#hearingCollection_0_hearingEstLengthNumType').select('Days');
    cy.get('#hearingCollection_0_hearingSitAlone-Sit\ Alone').select();
    cy.get('#hearingCollection_0_hearingSitAlone-Sit\ Alone').click();
    cy.get('#hearingCollection_0_hearingDateCollection .button').click();
    cy.get('#listedDate-day').type('21');
    cy.get('#listedDate-month').type('07');
    cy.get('#listedDate-year').type('2021');
    cy.get('#listedDate-hour').type('14');
    cy.get('.form-group > .button:nth-child(2)').click();
    cy.get('.form').submit();
    cy.get('.button:nth-child(2)').click();
    cy.get('.check-your-answers').submit();
})