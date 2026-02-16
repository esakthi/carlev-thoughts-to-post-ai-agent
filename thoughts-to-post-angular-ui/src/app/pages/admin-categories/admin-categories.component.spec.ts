import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminCategoriesComponent } from './admin-categories.component';
import { ThoughtsService } from '../../services/thoughts.service';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('AdminCategoriesComponent', () => {
  let component: AdminCategoriesComponent;
  let fixture: ComponentFixture<AdminCategoriesComponent>;
  let thoughtsServiceSpy: jasmine.SpyObj<ThoughtsService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('ThoughtsService', ['getFullCategories', 'createCategory', 'updateCategory', 'deleteCategory']);
    spy.getFullCategories.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AdminCategoriesComponent, CommonModule, FormsModule],
      providers: [{ provide: ThoughtsService, useValue: spy }]
    }).compileComponents();

    thoughtsServiceSpy = TestBed.inject(ThoughtsService) as jasmine.SpyObj<ThoughtsService>;
    fixture = TestBed.createComponent(AdminCategoriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories on init', () => {
    const mockCategories = [{ id: '1', category: 'Tech', searchDescription: '', modelRole: '' }];
    thoughtsServiceSpy.getFullCategories.and.returnValue(of(mockCategories));

    component.ngOnInit();

    expect(component.categories).toEqual(mockCategories);
    expect(thoughtsServiceSpy.getFullCategories).toHaveBeenCalled();
  });
});
