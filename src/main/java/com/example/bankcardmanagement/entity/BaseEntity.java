package com.example.bankcardmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@MappedSuperclass // Указывает, что это суперкласс для сущностей и его поля будут наследоваться
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Используем автоинкремент для ID
    private Long id;

    @CreationTimestamp // Автоматически устанавливает время создания
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Автоматически устанавливает время последнего обновления
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Переопределяем equals и hashCode для корректной работы с JPA и коллекциями
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        // Если id не null, сравниваем по id, иначе объекты считаются разными
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Используем getClass().hashCode() чтобы различать наследников с одинаковым id
        return id != null ? Objects.hash(getClass().hashCode(), id) : super.hashCode();
    }
}
