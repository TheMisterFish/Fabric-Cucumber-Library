@GameTestClient
Feature:
  Scenario: A client starts
    Given a client starts
    Then client has started

  @GameTestServer
  Scenario: A client starts2
    Given a client starts
    Then client has started
