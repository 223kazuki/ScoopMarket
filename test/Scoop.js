"use strict";

var Scoop = artifacts.require("Scoop");

contract('Scoop', ([owner, user1, user2, user3]) => {

    it("should scoop works successfully.", async () => {
        let scoop = await Scoop.new();
        await scoop.mint("Scoop1", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoop.mint("Scoop2", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoop.mint("Scoop3", 10**15, false, "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });

        let balance = await scoop.balanceOf(user1);
        console.log(balance.toNumber());

        let tokens = await scoop.scoopsOf(user1);
        console.log(tokens);

        let tokensForSale = await scoop.scoopsForSale();
        console.log(tokensForSale);

        await scoop.request(0, { from: user2, value: 10**14 });
        await scoop.approve(user2, 0, { from: user1 });
        await scoop.purchase(0, { from: user2, value: 10**15 });

        let balance2 = await scoop.balanceOf(user2);
        console.log(balance2.toNumber());
    });
});
