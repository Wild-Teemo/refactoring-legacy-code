package cn.xpbootcamp.legacy_code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;
import java.util.UUID;
import javax.transaction.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WalletTransactionTest {

  @Mock
  private DistributedLock distributedLock;
  @Mock
  private WalletService walletService;

  @Test
  void should_return_true_when_execute_transaction_success() throws InvalidTransactionException {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    given(distributedLock.lock(preAssignedId)).willReturn(true);
    given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount))
        .willReturn(UUID.randomUUID().toString());
    walletTransaction.setDistributedLock(distributedLock);
    walletTransaction.setWalletService(walletService);

    // when
    boolean executeResult = walletTransaction.execute();

    // then
    assertThat(executeResult).isTrue();
  }

  @Test
  void should_throw_invalid_transaction_exception_when_execute_with_null_buyer_id() {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    // when then
    assertThatExceptionOfType(InvalidTransactionException.class)
        .isThrownBy(walletTransaction::execute);
  }

  @Test
  void should_throw_invalid_transaction_exception_when_execute_with_null_seller_id() {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    // when then
    assertThatExceptionOfType(InvalidTransactionException.class)
        .isThrownBy(walletTransaction::execute);
  }

  @Test
  void should_throw_invalid_transaction_exception_when_execute_with_amount_less_than_0() {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    // when then
    assertThatExceptionOfType(InvalidTransactionException.class)
        .isThrownBy(walletTransaction::execute);
  }

  @Test
  void should_return_true_when_execute_with_status_is_already_execute()
      throws InvalidTransactionException {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    given(distributedLock.lock(preAssignedId)).willReturn(true);
    given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount))
        .willReturn(UUID.randomUUID().toString());
    walletTransaction.setDistributedLock(distributedLock);
    walletTransaction.setWalletService(walletService);
    walletTransaction.execute();

    // when
    boolean executeResult = walletTransaction.execute();

    // then
    assertThat(executeResult).isTrue();
  }

  @Test
  void should_return_false_when_transaction_distributed_lock_is_not_locked()
      throws InvalidTransactionException {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    given(distributedLock.lock(preAssignedId)).willReturn(false);
    walletTransaction.setDistributedLock(distributedLock);

    // when
    boolean executeResult = walletTransaction.execute();

    // then
    assertThat(executeResult).isFalse();
  }

  @Test
  void should_return_false_and_status_is_expire_when_transaction_over_20_days()
      throws InvalidTransactionException {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
    given(distributedLock.lock(preAssignedId)).willReturn(true);

    walletTransaction.setDistributedLock(distributedLock);
    walletTransaction.setCreatedTimestamp(19L);

    // when
    boolean executeResult = walletTransaction.execute();

    // then
    assertThat(executeResult).isFalse();
    assertThat(walletTransaction.getStatus()).isEqualTo(STATUS.EXPIRED);
  }

  @Test
  void should_return_false_when_execute_with_move_money_failed()
      throws InvalidTransactionException {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    long buyerId = 111L;
    long sellerId = 111L;
    double amount = 11.1;
    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

    given(distributedLock.lock(preAssignedId)).willReturn(true);
    given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount))
        .willReturn(null);

    walletTransaction.setDistributedLock(distributedLock);
    walletTransaction.setWalletService(walletService);

    // when
    boolean executeResult = walletTransaction.execute();

    // then
    assertThat(executeResult).isFalse();
    assertThat(walletTransaction.getStatus()).isEqualTo(STATUS.FAILED);
  }
}
