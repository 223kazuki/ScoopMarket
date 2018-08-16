"use strict";

var ScoopMarket = artifacts.require("ScoopMarket");

const checkReverted = async (promise) => {
    try {
        await promise;
    } catch (err) {
        assert(err.message.search("revert") >= 0)
        return;
    }
    assert.fail(0, 1, 'Expected throw not received');
}

contract('ScoopMarket', ([owner, user1, user2, user3]) => {
    let scoopMarket;
    let testHash = "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr";

    beforeEach('setup contract for each test', async function () {
        scoopMarket = await ScoopMarket.new({ from: owner });
    })

    // As a Proof of Existence dApp
    it("should mint scoops successfully.", async () => {
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop2", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop3", 10**15, false, testHash, { from: user1 });

        let balance = await scoopMarket.balanceOf(user1);
        assert(balance.toNumber() === 3, "Balance is correct.");

        let tokens = await scoopMarket.scoopsOf(user1);
        assert(tokens[0].toNumber() === 0, "Token 0 ID is correct.");
        assert(tokens[1].toNumber() === 1, "Token 1 ID is correct.");
        assert(tokens[2].toNumber() === 2, "Token 2 ID is correct.");

        let tokensForSale = await scoopMarket.scoopsForSale();
        assert(tokensForSale[0], "Token 0 ID is for sale.");
        assert(tokensForSale[1], "Token 1 ID is for sale.");
        assert(!tokensForSale[2], "Token 2 ID is not for sale.");

        // await scoopMarket.request(0, { from: user2, value: 10**14 });
        // await scoopMarket.approve(user2, 0, { from: user1 });
        // await scoopMarket.purchase(0, { from: user2, value: 10**15 });

        // let balance2 = await scoopMarket.balanceOf(user2);
        // console.log(balance2.toNumber());
    });
    it("should not mint scoop with insufficient value.", async () => {
        checkReverted(
            scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: 1 })
        );
    });
    it("only owner can set mint cost and withdraw it.", async () => {
        await scoopMarket.setMintCost(10**16, { from: owner });
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: 10**16 });
        checkReverted(
            scoopMarket.mint("scoop2", 10**15, true,  testHash, { from: user1 })
        );
        // let balance = await owner.getBalance();
        let balanceBefore = web3.eth.getBalance(owner);
        let result = await scoopMarket.withdrawPayments({ from: owner });
        let balanceAfter = web3.eth.getBalance(owner);
        let gasPrice = web3.eth.getTransaction(result.tx).gasPrice.toNumber();
        let cost = gasPrice * result.receipt.gasUsed;

        assert(balanceAfter.toNumber() - balanceBefore.toNumber() + cost === 10**16, "Balance is correct.");
    });
    it("should not mint scoop with invalid input parameters.", async () => {});
    it("should edit scoop successfully.", async () => {});
    it("should not edit scoop of others.", async () => {});

    // As a Market dApp
    it("should enable user to purchase only approved token.", async () => {});
    it("should.", async () => {});

    // System requirements
    it("should do emergency stop.", async () => {
        await scoopMarket.pause({ from: owner });
        checkReverted(
            scoopMarket.mint("scoop1", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr", { from: user1, value: 10**16 })
        );
    });

});
