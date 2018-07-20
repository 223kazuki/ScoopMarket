"use strict";

var Scoop = artifacts.require("Scoop");

contract('Scoop', ([owner, user1, user2]) => {
    it("should scoop works successfully.", async () => {
        let scoop = await Scoop.new();
        await scoop.mint("QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        await scoop.mint("QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user2, value: 10**16 });
        await scoop.mint("QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 });
        let balance = await scoop.balanceOf(user1);
        console.log(balance.toNumber());
        let tokens = await scoop.scoopsOf(user1);
        console.log(tokens);
        let tokenURI = await scoop.tokenURI(tokens[0].toNumber());
        console.log(tokenURI);
    });
});
