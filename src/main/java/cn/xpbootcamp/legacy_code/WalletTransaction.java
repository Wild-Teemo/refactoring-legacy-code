package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
  private static final long MAX_EXPIRED_MILLISECOND = 1728000000;
  private String id;
  private Long buyerId;
  private Long sellerId;
  private Long createdTimestamp;
  private Double amount;

  public void setCreatedTimestamp(Long createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  public STATUS getStatus() {
    return status;
  }

  private STATUS status;
  private DistributedLock distributedLock;
  private WalletService walletService;


  public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, Double amount) {
    if (preAssignedId != null && !preAssignedId.isEmpty()) {
      this.id = preAssignedId;
    } else {
      this.id = IdGenerator.generateTransactionId();
    }
    if (!this.id.startsWith("t_")) {
      this.id = "t_" + preAssignedId;
    }
    this.buyerId = buyerId;
    this.sellerId = sellerId;
    this.amount = amount;
    this.status = STATUS.TO_BE_EXECUTED;
    this.createdTimestamp = System.currentTimeMillis();
  }

  public boolean execute() throws InvalidTransactionException {
    validateOrder();
    if (isExecuted()) {
      return true;
    }
    boolean isLocked = false;
    try {
      isLocked = distributedLock.lock(id);

      // 锁定未成功，返回false
      if (!isLocked) {
        return false;
      }
      if (status == STATUS.EXECUTED) {
        return true; // double check
      }
      if (isExpired()) {
        this.status = STATUS.EXPIRED;
        return false;
      }

      String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
      status = walletTransactionId != null ? STATUS.EXECUTED : STATUS.FAILED;
      return STATUS.EXECUTED == status;
    } finally {
      if (isLocked) {
        distributedLock.unlock(id);
      }
    }
  }

  public void setDistributedLock(DistributedLock distributedLock) {
    this.distributedLock = distributedLock;
  }

  public void setWalletService(WalletService walletService) {
    this.walletService = walletService;
  }

  private void validateOrder() throws InvalidTransactionException {
    if (buyerId == null || (sellerId == null || amount < 0.0)) {
      throw new InvalidTransactionException("This is an invalid transaction");
    }
  }

  private boolean isExecuted() {
    return status == STATUS.EXECUTED;
  }

  private boolean isExpired() {
    return System.currentTimeMillis() - createdTimestamp > MAX_EXPIRED_MILLISECOND;
  }

}