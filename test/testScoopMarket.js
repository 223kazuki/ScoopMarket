"use strict";

var ScoopMarket = artifacts.require("ScoopMarket");

contract('ScoopMarket', ([owner, user1, user2, user3]) => {

    // As a Proof of Existence dApp
    it("should mint scoops tokens successfully.", async () => {
        let scoopMarket = await ScoopMarket.new();
        await scoopMarket.mint("scoop1", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoopMarket.mint("scoop2", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoopMarket.mint("scoop3", 10**15, false, "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });

        let balance = await scoopMarket.balanceOf(user1);
        console.log(balance.toNumber());

        let tokens = await scoopMarket.scoopsOf(user1);
        console.log(tokens);

        let tokensForSale = await scoopMarket.scoopsForSale();
        console.log(tokensForSale);

        await scoopMarket.request(0, { from: user2, value: 10**14 });
        await scoopMarket.approve(user2, 0, { from: user1 });
        await scoopMarket.purchase(0, { from: user2, value: 10**15 });

        let balance2 = await scoopMarket.balanceOf(user2);
        console.log(balance2.toNumber());
    });
    it("should not mint scoop with insufficient value.", async () => {});
    it("should edit scoop successfully.", async () => {});
    it("should not edit scoop of others.", async () => {});

    // As a Market dApp
    it("should enable user to purchase only approved token.", async () => {});
    it("should.", async () => {});

    // System requirements
    it("should do emergency stop.", async () => {});

});
