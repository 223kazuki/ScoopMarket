"use strict";

var ScoopMarket = artifacts.require("ScoopMarket");

contract('ScoopMarket', ([owner, user1, user2, user3]) => {

    it("should scoopMarket works successfully.", async () => {
        let scoopMarket = await ScoopMarket.new();
        await scoopMarket.mint("scoop1", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoopMarket.mint("scoop2", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoopMarket.mint("scoop3", 10**15, false, "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });

        let balance = await scoopMarket.balanceOf(user1);
        console.log(balance.toNumber());

        let tokens = await scoopMarket.scoopMarketsOf(user1);
        console.log(tokens);

        let tokensForSale = await scoopMarket.scoopMarketsForSale();
        console.log(tokensForSale);

        await scoopMarket.request(0, { from: user2, value: 10**14 });
        await scoopMarket.approve(user2, 0, { from: user1 });
        await scoopMarket.purchase(0, { from: user2, value: 10**15 });

        let balance2 = await scoopMarket.balanceOf(user2);
        console.log(balance2.toNumber());
    });
});
