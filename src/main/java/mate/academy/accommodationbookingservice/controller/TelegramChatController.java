package mate.academy.accommodationbookingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatResponseDto;
import mate.academy.accommodationbookingservice.service.telegram.TelegramChatService;

@Tag(name = "Telegram chat", description = "Provide telegram chat controlling")
@RequiredArgsConstructor
@RestController
@RequestMapping("/telegramChats")
public class TelegramChatController {
    private final TelegramChatService chatService;

    @Operation(description = "Select all chats where user start bot")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public Page<TelegramChatResponseDto> getAll(Pageable pageable) {
        return chatService.getAll(pageable);
    }

    @Operation(description = "Provide opportunity to receive notification for selected chat by id")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}")
    public TelegramChatResponseDto changeSubscriptionById(@PathVariable Long id) {
        return chatService.changeSubscriptionById(id);
    }

    @Operation(description = "Provide opportunity to delete chat by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        chatService.delete(id);
    }
}
