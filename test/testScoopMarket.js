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
    let zeroAddress = "0x0000000000000000000000000000000000000000";

    beforeEach('setup contract for each test', async function () {
        scoopMarket = await ScoopMarket.new({ from: owner });
    })

    // As a Proof of Existence dApp
    it("should mint scoops successfully.", async () => {
        await scoopMarket.mint("scoop0", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop2", 10**15, false, testHash, { from: user1 });

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

        {
            let scoop0 = await scoopMarket.scoop(0);
            let [id, name, _, imageURI, price, forSale, metaDataURI, author, owner, requestor] = scoop0;
            assert(id.toNumber() === 0, "scoop0 id is correct.");
            assert(name === "scoop0", "scoop0 name is correct.");
            assert(imageURI === testHash, "scoop0 imageURI is correct.");
            assert(price.toNumber() === 10**15, "scoop0 price is correct.");
            assert(forSale, "scoop0 forSale is correct.");
            assert(metaDataURI === "", "scoop0 metaDataURI is correct.");
            assert(author === user1, "scoop0 author is correct.");
            assert(owner === user1, "scoop0 owner is correct.");
            assert(requestor === zeroAddress, "scoop0 requestor is correct.");
        }
        {
            let scoop1 = await scoopMarket.scoop(1);
            let [id, name, _, imageURI, price, forSale, metaDataURI, author, owner, requestor] = scoop1;
            assert(id.toNumber() === 1, "scoop1 id is correct.");
            assert(name === "scoop1", "scoop1 name is correct.");
            assert(imageURI === testHash, "scoop1 imageURI is correct.");
            assert(price.toNumber() === 10**15, "scoop1 price is correct.");
            assert(forSale, "scoop1 forSale is correct.");
            assert(metaDataURI === "", "scoop1 metaDataURI is correct.");
            assert(author === user1, "scoop1 author is correct.");
            assert(owner === user1, "scoop1 owner is correct.");
            assert(requestor === zeroAddress, "scoop1 requestor is correct.");
        }
        {
            let scoop2 = await scoopMarket.scoop(2);
            let [id, name, _, imageURI, price, forSale, metaDataURI, author, owner, requestor] = scoop2;
            assert(id.toNumber() === 2, "scoop2 id is correct.");
            assert(name === "scoop2", "scoop2 name is correct.");
            assert(imageURI === testHash, "scoop2 imageURI is correct.");
            assert(price.toNumber() === 10**15, "scoop2 price is correct.");
            assert(!forSale, "scoop2 forSale is correct.");
            assert(metaDataURI === "", "scoop2 metaDataURI is correct.");
            assert(author === user1, "scoop2 author is correct.");
            assert(owner === user1, "scoop2 owner is correct.");
            assert(requestor === zeroAddress, "scoop2 requestor is correct.");
        }
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
        let balanceBefore = web3.eth.getBalance(owner);
        await scoopMarket.withdrawPayments({ from: owner });
        let balanceAfter = web3.eth.getBalance(owner);
        assert(balanceAfter.toNumber() > balanceBefore.toNumber(), "Withdrawal correctly.");
    });
    it("should not mint scoop with invalid input parameters.", async () => {});
    it("should edit scoop successfully.", async () => {
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1 });
    });
    it("should not edit scoop of others.", async () => {});

    // As a Market dApp
    it("buyer should be able to purchase only approved token.", async () => {
        // await scoopMarket.request(0, { from: user2, value: 10**14 });
        // await scoopMarket.approve(user2, 0, { from: user1 });
        // await scoopMarket.purchase(0, { from: user2, value: 10**15 });

        // let balance2 = await scoopMarket.balanceOf(user2);
        // console.log(balance2.toNumber());
    });
    it("buyer should be able to request only token for sale.", async () => {});
    it("buyer should be able to cancel request.", async () => {});
    it("buyer should be able to cancel approval.", async () => {});
    it("seller should be able to approve request.", async () => {});
    it("seller should be able to deny request.", async () => {});

    // System requirements
    it("should do emergency stop.", async () => {
        await scoopMarket.pause({ from: owner });
        checkReverted(
        );
    });

});
