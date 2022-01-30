package backoffice.application.office_holder;

import authentication.domain.user.UserCreatedEvent;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestOfficeHolderCreator {

    OfficeHolderRepository mockOfficeHolderRepository = mock(OfficeHolderRepository.class);
    OfficeHolderCreator creator = new OfficeHolderCreator(mockOfficeHolderRepository);
    ArgumentCaptor<OfficeHolder> officeHolderCaptor = ArgumentCaptor.forClass(OfficeHolder.class);

    @Test
    void itShouldStoreOfficeHolderWithInformationSpecified() {
        OfficeHolderId id = new OfficeHolderId();

        creator.createOfficeHolderFromEvent(new UserCreatedEvent(id.toString(), "john@mail.com", "OFFICE_HOLDER"));

        verify(mockOfficeHolderRepository).store(officeHolderCaptor.capture());
        OfficeHolder officeHolderCalled = officeHolderCaptor.getValue();
        assertThat(officeHolderCalled).isEqualTo(new OfficeHolder(id, "john@mail.com"));
    }

    @Test
    void itShouldNotCreateOfficeHolderWhenUserTypeIsNotOfficeHolder() {
        OfficeHolderId id = new OfficeHolderId();

        creator.createOfficeHolderFromEvent(new UserCreatedEvent(id.toString(), "john@mail.com", "RENTER"));

        verify(mockOfficeHolderRepository, times(0)).store(any());
    }

}
