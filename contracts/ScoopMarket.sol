pragma solidity ^0.4.23;

import "openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "openzeppelin-solidity/contracts/math/SafeMath.sol";
import "openzeppelin-solidity/contracts/lifecycle/Pausable.sol";
import "openzeppelin-solidity/contracts/payment/PullPayment.sol";

/** 
 * @title Scoop Market
 * @author Kazuki Tsutusmi
 * @notice A contract to publish and trade "Scoop".
 * @dev This is a ERC-721 token implementation.
 */
contract ScoopMarket is ERC721Token, Pausable, PullPayment {
  // Libraries =============================================================================
  // @dev This library is not from EthPM but usage is similer to EthPM.
  using SafeMath for uint;

  // Events ================================================================================
  // @dev Approval event is defined in ERC721Basic.
  event Minted(string _name, uint _price, bool _forSale, string _imageHash);
  event MintCostSet(uint _mintCost);
  event TokenEdited(uint _tokenID, string _name, uint _price, bool _forSale, string _metaDataHash);
  event Requested(uint _tokenID);
  event Canceled(uint _tokenID);
  event Denied(uint _tokenID);
  event Purchased(uint _tokenID);
  event PaymentsWithdrawed();

  // Structs ===============================================================================
  struct Scoop {
    string  name;         // Token name.
    uint    timestamp;    // When it was minted.
    string  imageHash;    // IPFS hash of the scoop image.
    address author;       // Address who minted this token.
    uint    price;        // The price of the token in wei.
    bool    forSale;      // True if this scoop is for sale.
    string  metaDataHash; // IPFS hash of the scoop meta data.
    address requestor;    // Address who is requesting to purchase this token.
  }

  // Variables =============================================================================
  Scoop[] public scoops;
  uint public mintCost;

  // Modifiers =============================================================================
  /**
   * @param _tokenID ID of target token.
   * @dev require target token is for sale.
   */
  modifier onlyForSaleToken(uint _tokenID) {
    require(scoops[_tokenID].forSale, "Only for sale token.");
    _;
  }

 /**
   * @param _tokenID ID of target token.
   * @dev require target token exists.
   */
  modifier tokenExists(uint _tokenID) {
    require(exists(_tokenID), "Token doesn't exist.");
    _;
  }

  // Functions ============================================================================  
  constructor() public ERC721Token("Scoop", "SCP") {}

  /**
   * @notice It costs mintCost value.
   * @param _name The name of scoop. Its length must between 1 to 50.
   * @param _price The price of the token in wei.
   * @param _forSale True if this scoop is for sale.
   * @param _imageHash IPFS hash of the scoop image. Its length must be 46.
   */
  function mint(string _name, uint _price, bool _forSale, string _imageHash) external payable whenNotPaused {
    // Check value.
    require(msg.value == mintCost, "Mint cost is insufficient.");

    // Check input parameters.
    uint _nameLength = bytes(_name).length;
    require(_nameLength > 0 && _nameLength <= 50, "Name length must be between 1 to 50");
    uint _imageHashLength = bytes(_imageHash).length;
    require(_imageHashLength == 46, "Image hash length must be 46.");

    // metaDataHash and requestor set to empty.
    Scoop memory _scoop = Scoop(_name, block.timestamp, _imageHash, msg.sender, _price, _forSale, "", address(0));
    uint _tokenID = scoops.push(_scoop).sub(1);
    super._mint(msg.sender, _tokenID);

    // Send value to seller by PullPayments contract.
    asyncTransfer(owner, msg.value);
    emit Minted(_name, _price, _forSale, _imageHash);
  }

  /**
   * @param _mintCost cost to mint.
   */
  function setMintCost(uint _mintCost) external onlyOwner {
    mintCost = _mintCost;
    emit MintCostSet(_mintCost);
  }
  
  /**
   * @param _address User address.
   * @return tokenIDs: IDs of the tokens owned by _address.
   */
  function scoopsOf(address _address) external view returns (uint[]) {
    return ownedTokens[_address];
  }

  /**
   * @return forSales: The forsale array of tokens. Its indexes are the token ids. 
   */  
  function scoopsForSale() external view returns (bool[]) {
    bool[] memory _result = new bool[](scoops.length);
    for (uint i = 0; i < scoops.length; i ++) {
      _result[i] = scoops[i].forSale;
    }
    return _result;
  }

  /**
   * @param _tokenID ID of target token.
   * @param _name The name of scoop. Its length must between 1 to 50.
   * @param _price The price of the token in wei.
   * @param _forSale True if this scoop is for sale.
   * @param _metaDataHash IPFS hash of the scoop meta data. Its length must be 46.
   */  
  function editToken(uint _tokenID, string _name, uint _price, bool _forSale, string _metaDataHash) 
    external tokenExists(_tokenID) onlyOwnerOf(_tokenID) whenNotPaused {
    // Check input parameters.
    uint _nameLength = bytes(_name).length;
    require(_nameLength > 0 && _nameLength <= 50, "Name length must be between 1 to 50");
    uint _metaDataHashLength = bytes(_metaDataHash).length;
    require(_metaDataHashLength == 46, "Meta data hash length must be 46.");

    Scoop storage _scoop = scoops[_tokenID];
    _scoop.name = _name;
    _scoop.price = _price;
    _scoop.forSale = _forSale;
    _scoop.metaDataHash = _metaDataHash;
    emit TokenEdited(_tokenID, _name, _price, _forSale, _metaDataHash);
  }
  
  /**
   * @return tokenID      : Token id.
   * @return name         : Token name.
   * @return timestamp    : When it was minted.
   * @return imageHash    : IPFS hash of the scoop image.
   * @return price        : The price of the token in wei.
   * @return forSale      : True if this scoop is for sale.
   * @return metaDataHash : IPFS hash of the scoop meta data.
   * @return author       : Address who minted this token.
   * @return owner        : Address who owns this token.
   * @return requestor    : Address who is requesting to purchase this token.
   */
  function scoop(uint _tokenID) external view returns (uint, string, uint, string, uint, bool, string, address, address, address) {
    Scoop memory _scoop = scoops[_tokenID];
    address _owner = tokenOwner[_tokenID];
    
    return (_tokenID, _scoop.name, _scoop.timestamp, _scoop.imageHash, _scoop.price, _scoop.forSale, 
      _scoop.metaDataHash, _scoop.author, _owner, _scoop.requestor);
  }
  
  /**
   * @notice Exected to be used by token buyer.
   * @notice Request to purchase this token.
   * @param _tokenID ID of target token.
   */
  function request(uint _tokenID) external payable onlyForSaleToken(_tokenID) tokenExists(_tokenID) whenNotPaused {
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.requestor = msg.sender;
    emit Requested(_tokenID);
  }
  
  /**
   * @notice Exected to be used by token buyer.
   * @notice Cancel request or approval to purchase this token.
   * @param _tokenID ID of target token.
   */
  function cancel(uint _tokenID) external payable tokenExists(_tokenID) whenNotPaused {
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.requestor = address(0);
    super.clearApproval(tokenOwner[_tokenID], _tokenID);
    emit Canceled(_tokenID);
  }
  
  /**
   * @notice Exected to be used by token owner(seller).
   * @notice Approve to purchase this token.
   * @param _to Whom you allow to purchase this token.
   * @param _tokenID ID of target token.
   */
  function approve(address _to, uint _tokenID) public tokenExists(_tokenID)  onlyOwnerOf(_tokenID) onlyForSaleToken(_tokenID) whenNotPaused {
    require(_to == scoops[_tokenID].requestor, "Tried to approve non requestor.");
    super.approve(_to, _tokenID);
  }
  
  /**
   * @notice Exected to be used by token owner(seller).
   * @notice Deny to purchase this token.
   * @param _tokenID ID of target token.
   */
  function deny(uint _tokenID) external payable tokenExists(_tokenID)  onlyOwnerOf(_tokenID) whenNotPaused {
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.requestor = address(0);
    emit Denied(_tokenID);
  }
  
  /**
   * @notice Exected to be used by token buyer.
   * @notice Purchase token which approved.
   * @param _tokenID ID of target token.
   */
  function purchase(uint _tokenID) external payable tokenExists(_tokenID) onlyForSaleToken(_tokenID) whenNotPaused {
    address seller = tokenOwner[_tokenID];
    address buyer = msg.sender;
    require(seller != buyer, "Can't transfer to myself.");
    require(getApproved(_tokenID) == buyer, "sender is not approved.");
    require(scoops[_tokenID].price == msg.value, "Value is insufficient.");

    // Transfer token.
    safeTransferFrom(seller, buyer, _tokenID);

    // Update token not for sale.
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.forSale = false;
    _scoop.requestor = address(0);

    // Send value to seller by PullPayments contract.
    if (_scoop.price > 0) {
      asyncTransfer(seller, _scoop.price);
    }
    emit Purchased(_tokenID);
  }

  /**
   * @dev This overwrites PullPayment.withdrawPayments to enable to pause.
   */
  function withdrawPayments() public whenNotPaused {
    super.withdrawPayments();
    emit PaymentsWithdrawed();
  }
}