// 台幣活存（含支存）
List<CustomerAssetsDto.TwdDemandDeposit> twdDemandDeposit = accountInfoList.stream()
        .filter(Objects::nonNull)
        .filter((AccountInfo data ->
                "0".equals(data.accountStatus())
                && "1".equals(data.accountBusinessType())
                && "T".equals(data.currencyType())))
        .map(
                AccountInfo data ->
                        new CustomerAssetsDto.TwdDemandDeposit(
                                data.account(),
                                data.realBalance(),
                                data.availableBalance()
                        )
        ).toList();

BigDecimal twdDemandDepositRealBalanceTotal = sum(twdDemandDeposit.stream().map(CustomerAssetsDto.TwdDemandDeposit::realBalance).toList());
BigDecimal twdDemandDepositAvailableBalanceTotal = sum(twdDemandDeposit.stream().map(CustomerAssetsDto.TwdDemandDeposit::availableBalance).toList());
BigDecimal lumTotalAmount = sum(lumPairList.stream().map(Pair::getLeft).toList());

// 外幣活存
List<CustomerAssetsDto.FxDemandDeposit> fxDemandDeposit = accountInfoList.stream()
        .filter(Objects::nonNull)
        .filter(AccountInfo accountInfo ->
                "0".equals(accountInfo.accountStatus())
                && "1".equals(accountInfo.accountBusinessType())
                && "F".equals(accountInfo.currencyType()))
        .flatMap(
                AccountInfo accountInfo ->
                        accountInfo.currInfoList().stream()





List<CustomerAssetsDto.LuM> lumList = new ArrayList<>();
for (int i = 0; i < LUM_PRODUCT_CATEGORY.size(); i++) {
    lumList.add(