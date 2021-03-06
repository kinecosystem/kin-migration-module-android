package kin.sdk.migration;


final class IntegConsts {


	static final String TEST_SDK_NETWORK_URL = "https://horizon-testnet.kininfrastructure.com/";
	static final String TEST_SDK_NETWORK_ID = "Kin Testnet ; December 2018";
	static final String TEST_SDK_URL_CREATE_ACCOUNT = "https://friendbot-testnet.kininfrastructure.com?addr=%s&amount="
		+ 0; // zero because the default is currently 10000
	static final String TEST_SDK_URL_FUND = "https://friendbot-testnet.kininfrastructure.com/fund?addr=%s&amount=";

	static final String TEST_CORE_NETWORK_URL = "https://horizon-playground.kininfrastructure.com/";
	static final String TEST_CORE_NETWORK_ID = "Kin Playground Network ; June 2018";
	static final String TEST_CORE_URL_CREATE_ACCOUNT = "https://friendbot-playground.kininfrastructure.com/?addr=";
	static final String TEST_CORE_URL_FUND = "http://faucet-playground.kininfrastructure.com/fund?account=%s&amount=";

}
